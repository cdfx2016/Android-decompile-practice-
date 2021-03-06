package com.google.android.exoplayer2.trackselection;

import android.os.Handler;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection.Factory;
import com.google.android.exoplayer2.util.Util;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class MappingTrackSelector extends TrackSelector<MappedTrackInfo> {
    private final SparseBooleanArray rendererDisabledFlags = new SparseBooleanArray();
    private final SparseArray<Map<TrackGroupArray, SelectionOverride>> selectionOverrides = new SparseArray();

    public static final class MappedTrackInfo {
        public static final int RENDERER_SUPPORT_NO_TRACKS = 0;
        public static final int RENDERER_SUPPORT_PLAYABLE_TRACKS = 2;
        public static final int RENDERER_SUPPORT_UNPLAYABLE_TRACKS = 1;
        private final int[][][] formatSupport;
        private final int[] mixedMimeTypeAdaptiveSupport;
        private final int rendererCount;
        private final int[] rendererTrackTypes;
        private final TrackGroupArray[] trackGroups;
        private final TrackGroupArray unassociatedTrackGroups;

        MappedTrackInfo(int[] rendererTrackTypes, TrackGroupArray[] trackGroups, int[] mixedMimeTypeAdaptiveSupport, int[][][] formatSupport, TrackGroupArray unassociatedTrackGroups) {
            this.rendererTrackTypes = rendererTrackTypes;
            this.trackGroups = trackGroups;
            this.formatSupport = formatSupport;
            this.mixedMimeTypeAdaptiveSupport = mixedMimeTypeAdaptiveSupport;
            this.unassociatedTrackGroups = unassociatedTrackGroups;
            this.rendererCount = trackGroups.length;
        }

        public TrackGroupArray getTrackGroups(int rendererIndex) {
            return this.trackGroups[rendererIndex];
        }

        public int getRendererSupport(int rendererIndex) {
            boolean hasTracks = false;
            int[][] rendererFormatSupport = this.formatSupport[rendererIndex];
            for (int i = 0; i < rendererFormatSupport.length; i++) {
                for (int i2 : rendererFormatSupport[i]) {
                    hasTracks = true;
                    if ((i2 & 3) == 3) {
                        return 2;
                    }
                }
            }
            return hasTracks ? 1 : 0;
        }

        public int getTrackFormatSupport(int rendererIndex, int groupIndex, int trackIndex) {
            return this.formatSupport[rendererIndex][groupIndex][trackIndex] & 3;
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, boolean includeCapabilitiesExceededTracks) {
            int trackCount = this.trackGroups[rendererIndex].get(groupIndex).length;
            int[] trackIndices = new int[trackCount];
            int i = 0;
            int trackIndexCount = 0;
            while (i < trackCount) {
                int trackIndexCount2;
                int fixedSupport = getTrackFormatSupport(rendererIndex, groupIndex, i);
                if (fixedSupport == 3 || (includeCapabilitiesExceededTracks && fixedSupport == 2)) {
                    trackIndexCount2 = trackIndexCount + 1;
                    trackIndices[trackIndexCount] = i;
                } else {
                    trackIndexCount2 = trackIndexCount;
                }
                i++;
                trackIndexCount = trackIndexCount2;
            }
            return getAdaptiveSupport(rendererIndex, groupIndex, Arrays.copyOf(trackIndices, trackIndexCount));
        }

        public int getAdaptiveSupport(int rendererIndex, int groupIndex, int[] trackIndices) {
            int handledTrackCount = 0;
            int adaptiveSupport = 8;
            boolean multipleMimeTypes = false;
            String firstSampleMimeType = null;
            int i = 0;
            while (i < trackIndices.length) {
                String sampleMimeType = this.trackGroups[rendererIndex].get(groupIndex).getFormat(trackIndices[i]).sampleMimeType;
                int handledTrackCount2 = handledTrackCount + 1;
                if (handledTrackCount == 0) {
                    firstSampleMimeType = sampleMimeType;
                } else {
                    multipleMimeTypes |= !Util.areEqual(firstSampleMimeType, sampleMimeType) ? 1 : 0;
                }
                adaptiveSupport = Math.min(adaptiveSupport, this.formatSupport[rendererIndex][groupIndex][i] & 12);
                i++;
                handledTrackCount = handledTrackCount2;
            }
            if (multipleMimeTypes) {
                return Math.min(adaptiveSupport, this.mixedMimeTypeAdaptiveSupport[rendererIndex]);
            }
            return adaptiveSupport;
        }

        public TrackGroupArray getUnassociatedTrackGroups() {
            return this.unassociatedTrackGroups;
        }

        public boolean hasOnlyUnplayableTracks(int trackType) {
            int rendererSupport = 0;
            for (int i = 0; i < this.rendererCount; i++) {
                if (this.rendererTrackTypes[i] == trackType) {
                    rendererSupport = Math.max(rendererSupport, getRendererSupport(i));
                }
            }
            if (rendererSupport == 1) {
                return true;
            }
            return false;
        }
    }

    public static final class SelectionOverride {
        public final Factory factory;
        public final int groupIndex;
        public final int length;
        public final int[] tracks;

        public SelectionOverride(Factory factory, int groupIndex, int... tracks) {
            this.factory = factory;
            this.groupIndex = groupIndex;
            this.tracks = tracks;
            this.length = tracks.length;
        }

        public TrackSelection createTrackSelection(TrackGroupArray groups) {
            return this.factory.createTrackSelection(groups.get(this.groupIndex), this.tracks);
        }

        public boolean containsTrack(int track) {
            for (int i : this.tracks) {
                if (i == track) {
                    return true;
                }
            }
            return false;
        }
    }

    protected abstract TrackSelection[] selectTracks(RendererCapabilities[] rendererCapabilitiesArr, TrackGroupArray[] trackGroupArrayArr, int[][][] iArr) throws ExoPlaybackException;

    public MappingTrackSelector(Handler eventHandler) {
        super(eventHandler);
    }

    public final void setRendererDisabled(int rendererIndex, boolean disabled) {
        if (this.rendererDisabledFlags.get(rendererIndex) != disabled) {
            this.rendererDisabledFlags.put(rendererIndex, disabled);
            invalidate();
        }
    }

    public final boolean getRendererDisabled(int rendererIndex) {
        return this.rendererDisabledFlags.get(rendererIndex);
    }

    public final void setSelectionOverride(int rendererIndex, TrackGroupArray groups, SelectionOverride override) {
        Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
        if (overrides == null) {
            overrides = new HashMap();
            this.selectionOverrides.put(rendererIndex, overrides);
        }
        if (!overrides.containsKey(groups) || !Util.areEqual(overrides.get(groups), override)) {
            overrides.put(groups, override);
            invalidate();
        }
    }

    public final boolean hasSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
        return overrides != null && overrides.containsKey(groups);
    }

    public final SelectionOverride getSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
        return overrides != null ? (SelectionOverride) overrides.get(groups) : null;
    }

    public final void clearSelectionOverride(int rendererIndex, TrackGroupArray groups) {
        Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(rendererIndex);
        if (overrides != null && overrides.containsKey(groups)) {
            overrides.remove(groups);
            if (overrides.isEmpty()) {
                this.selectionOverrides.remove(rendererIndex);
            }
            invalidate();
        }
    }

    public final void clearSelectionOverrides(int rendererIndex) {
        Map<TrackGroupArray, ?> overrides = (Map) this.selectionOverrides.get(rendererIndex);
        if (overrides != null && !overrides.isEmpty()) {
            this.selectionOverrides.remove(rendererIndex);
            invalidate();
        }
    }

    public final void clearSelectionOverrides() {
        if (this.selectionOverrides.size() != 0) {
            this.selectionOverrides.clear();
            invalidate();
        }
    }

    public final TrackSelections<MappedTrackInfo> selectTracks(RendererCapabilities[] rendererCapabilities, TrackGroupArray trackGroups) throws ExoPlaybackException {
        int i;
        int[] rendererTrackGroupCounts = new int[(rendererCapabilities.length + 1)];
        TrackGroup[][] rendererTrackGroups = new TrackGroup[(rendererCapabilities.length + 1)][];
        int[][][] rendererFormatSupports = new int[(rendererCapabilities.length + 1)][][];
        for (i = 0; i < rendererTrackGroups.length; i++) {
            rendererTrackGroups[i] = new TrackGroup[trackGroups.length];
            rendererFormatSupports[i] = new int[trackGroups.length][];
        }
        int[] mixedMimeTypeAdaptationSupport = getMixedMimeTypeAdaptationSupport(rendererCapabilities);
        for (int groupIndex = 0; groupIndex < trackGroups.length; groupIndex++) {
            int[] rendererFormatSupport;
            TrackGroup group = trackGroups.get(groupIndex);
            int rendererIndex = findRenderer(rendererCapabilities, group);
            if (rendererIndex == rendererCapabilities.length) {
                rendererFormatSupport = new int[group.length];
            } else {
                rendererFormatSupport = getFormatSupport(rendererCapabilities[rendererIndex], group);
            }
            int rendererTrackGroupCount = rendererTrackGroupCounts[rendererIndex];
            rendererTrackGroups[rendererIndex][rendererTrackGroupCount] = group;
            rendererFormatSupports[rendererIndex][rendererTrackGroupCount] = rendererFormatSupport;
            rendererTrackGroupCounts[rendererIndex] = rendererTrackGroupCounts[rendererIndex] + 1;
        }
        TrackGroupArray[] rendererTrackGroupArrays = new TrackGroupArray[rendererCapabilities.length];
        int[] rendererTrackTypes = new int[rendererCapabilities.length];
        for (i = 0; i < rendererCapabilities.length; i++) {
            rendererTrackGroupCount = rendererTrackGroupCounts[i];
            rendererTrackGroupArrays[i] = new TrackGroupArray((TrackGroup[]) Arrays.copyOf(rendererTrackGroups[i], rendererTrackGroupCount));
            rendererFormatSupports[i] = (int[][]) Arrays.copyOf(rendererFormatSupports[i], rendererTrackGroupCount);
            rendererTrackTypes[i] = rendererCapabilities[i].getTrackType();
        }
        TrackGroupArray unassociatedTrackGroupArray = new TrackGroupArray((TrackGroup[]) Arrays.copyOf(rendererTrackGroups[rendererCapabilities.length], rendererTrackGroupCounts[rendererCapabilities.length]));
        TrackSelection[] trackSelections = selectTracks(rendererCapabilities, rendererTrackGroupArrays, rendererFormatSupports);
        for (i = 0; i < rendererCapabilities.length; i++) {
            if (this.rendererDisabledFlags.get(i)) {
                trackSelections[i] = null;
            } else {
                TrackGroupArray rendererTrackGroup = rendererTrackGroupArrays[i];
                Map<TrackGroupArray, SelectionOverride> overrides = (Map) this.selectionOverrides.get(i);
                SelectionOverride override = overrides == null ? null : (SelectionOverride) overrides.get(rendererTrackGroup);
                if (override != null) {
                    trackSelections[i] = override.createTrackSelection(rendererTrackGroup);
                }
            }
        }
        return new TrackSelections(new MappedTrackInfo(rendererTrackTypes, rendererTrackGroupArrays, mixedMimeTypeAdaptationSupport, rendererFormatSupports, unassociatedTrackGroupArray), trackSelections);
    }

    private static int findRenderer(RendererCapabilities[] rendererCapabilities, TrackGroup group) throws ExoPlaybackException {
        int bestRendererIndex = rendererCapabilities.length;
        int bestSupportLevel = 0;
        for (int rendererIndex = 0; rendererIndex < rendererCapabilities.length; rendererIndex++) {
            RendererCapabilities rendererCapability = rendererCapabilities[rendererIndex];
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                int trackSupportLevel = rendererCapability.supportsFormat(group.getFormat(trackIndex));
                if (trackSupportLevel > bestSupportLevel) {
                    bestRendererIndex = rendererIndex;
                    bestSupportLevel = trackSupportLevel;
                    if (bestSupportLevel == 3) {
                        return bestRendererIndex;
                    }
                }
            }
        }
        return bestRendererIndex;
    }

    private static int[] getFormatSupport(RendererCapabilities rendererCapabilities, TrackGroup group) throws ExoPlaybackException {
        int[] formatSupport = new int[group.length];
        for (int i = 0; i < group.length; i++) {
            formatSupport[i] = rendererCapabilities.supportsFormat(group.getFormat(i));
        }
        return formatSupport;
    }

    private static int[] getMixedMimeTypeAdaptationSupport(RendererCapabilities[] rendererCapabilities) throws ExoPlaybackException {
        int[] mixedMimeTypeAdaptationSupport = new int[rendererCapabilities.length];
        for (int i = 0; i < mixedMimeTypeAdaptationSupport.length; i++) {
            mixedMimeTypeAdaptationSupport[i] = rendererCapabilities[i].supportsMixedMimeTypeAdaptation();
        }
        return mixedMimeTypeAdaptationSupport;
    }
}
