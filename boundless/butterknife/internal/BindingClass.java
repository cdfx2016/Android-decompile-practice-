package butterknife.internal;

import cn.finalteam.toolsfinal.io.FilenameUtils;
import com.fanyu.boundless.util.FileUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final class BindingClass {
    private final String className;
    private final String classPackage;
    private final Map<FieldCollectionViewBinding, int[]> collectionBindings = new LinkedHashMap();
    private String parentViewBinder;
    private final List<FieldResourceBinding> resourceBindings = new ArrayList();
    private final String targetClass;
    private final Map<Integer, ViewBindings> viewIdMap = new LinkedHashMap();

    BindingClass(String classPackage, String className, String targetClass) {
        this.classPackage = classPackage;
        this.className = className;
        this.targetClass = targetClass;
    }

    void addField(int id, FieldViewBinding binding) {
        getOrCreateViewBindings(id).addFieldBinding(binding);
    }

    void addFieldCollection(int[] ids, FieldCollectionViewBinding binding) {
        this.collectionBindings.put(binding, ids);
    }

    boolean addMethod(int id, ListenerClass listener, ListenerMethod method, MethodViewBinding binding) {
        ViewBindings viewBindings = getOrCreateViewBindings(id);
        if (viewBindings.hasMethodBinding(listener, method) && !"void".equals(method.returnType())) {
            return false;
        }
        viewBindings.addMethodBinding(listener, method, binding);
        return true;
    }

    void addResource(FieldResourceBinding binding) {
        this.resourceBindings.add(binding);
    }

    void setParentViewBinder(String parentViewBinder) {
        this.parentViewBinder = parentViewBinder;
    }

    ViewBindings getViewBinding(int id) {
        return (ViewBindings) this.viewIdMap.get(Integer.valueOf(id));
    }

    private ViewBindings getOrCreateViewBindings(int id) {
        ViewBindings viewId = (ViewBindings) this.viewIdMap.get(Integer.valueOf(id));
        if (viewId != null) {
            return viewId;
        }
        viewId = new ViewBindings(id);
        this.viewIdMap.put(Integer.valueOf(id), viewId);
        return viewId;
    }

    String getFqcn() {
        return this.classPackage + FileUtil.FILE_EXTENSION_SEPARATOR + this.className;
    }

    String brewJava() {
        StringBuilder builder = new StringBuilder();
        builder.append("// Generated code from Butter Knife. Do not modify!\n");
        builder.append("package ").append(this.classPackage).append(";\n\n");
        if (!this.resourceBindings.isEmpty()) {
            builder.append("import android.content.res.Resources;\n");
        }
        if (!(this.viewIdMap.isEmpty() && this.collectionBindings.isEmpty())) {
            builder.append("import android.view.View;\n");
        }
        builder.append("import butterknife.ButterKnife.Finder;\n");
        if (this.parentViewBinder == null) {
            builder.append("import butterknife.ButterKnife.ViewBinder;\n");
        }
        builder.append('\n');
        builder.append("public class ").append(this.className);
        builder.append("<T extends ").append(this.targetClass).append(">");
        if (this.parentViewBinder != null) {
            builder.append(" extends ").append(this.parentViewBinder).append("<T>");
        } else {
            builder.append(" implements ViewBinder<T>");
        }
        builder.append(" {\n");
        emitBindMethod(builder);
        builder.append('\n');
        emitUnbindMethod(builder);
        builder.append("}\n");
        return builder.toString();
    }

    private void emitBindMethod(StringBuilder builder) {
        builder.append("  @Override ").append("public void bind(final Finder finder, final T target, Object source) {\n");
        if (this.parentViewBinder != null) {
            builder.append("    super.bind(finder, target, source);\n\n");
        }
        if (!(this.viewIdMap.isEmpty() && this.collectionBindings.isEmpty())) {
            builder.append("    View view;\n");
            for (ViewBindings bindings : this.viewIdMap.values()) {
                emitViewBindings(builder, bindings);
            }
            for (Entry<FieldCollectionViewBinding, int[]> entry : this.collectionBindings.entrySet()) {
                emitCollectionBinding(builder, (FieldCollectionViewBinding) entry.getKey(), (int[]) entry.getValue());
            }
        }
        if (!this.resourceBindings.isEmpty()) {
            builder.append("    Resources res = finder.getContext(source).getResources();\n");
            for (FieldResourceBinding binding : this.resourceBindings) {
                builder.append("    target.").append(binding.getName()).append(" = res.").append(binding.getMethod()).append('(').append(binding.getId()).append(");\n");
            }
        }
        builder.append("  }\n");
    }

    private void emitCollectionBinding(StringBuilder builder, FieldCollectionViewBinding binding, int[] ids) {
        builder.append("    target.").append(binding.getName()).append(" = ");
        switch (binding.getKind()) {
            case ARRAY:
                builder.append("Finder.arrayOf(");
                break;
            case LIST:
                builder.append("Finder.listOf(");
                break;
            default:
                throw new IllegalStateException("Unknown kind: " + binding.getKind());
        }
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append("\n        finder.<").append(binding.getType()).append(">").append(binding.isRequired() ? "findRequiredView" : "findOptionalView").append("(source, ").append(ids[i]).append(", \"");
            emitHumanDescription(builder, Collections.singleton(binding));
            builder.append("\")");
        }
        builder.append("\n    );\n");
    }

    private void emitViewBindings(StringBuilder builder, ViewBindings bindings) {
        builder.append("    view = ");
        List<ViewBinding> requiredViewBindings = bindings.getRequiredBindings();
        if (requiredViewBindings.isEmpty()) {
            builder.append("finder.findOptionalView(source, ").append(bindings.getId()).append(", null);\n");
        } else if (bindings.getId() == -1) {
            builder.append("target;\n");
        } else {
            builder.append("finder.findRequiredView(source, ").append(bindings.getId()).append(", \"");
            emitHumanDescription(builder, requiredViewBindings);
            builder.append("\");\n");
        }
        emitFieldBindings(builder, bindings);
        emitMethodBindings(builder, bindings);
    }

    private void emitFieldBindings(StringBuilder builder, ViewBindings bindings) {
        Collection<FieldViewBinding> fieldBindings = bindings.getFieldBindings();
        if (!fieldBindings.isEmpty()) {
            for (FieldViewBinding fieldBinding : fieldBindings) {
                builder.append("    target.").append(fieldBinding.getName()).append(" = ");
                if (fieldBinding.requiresCast()) {
                    builder.append("finder.castView(view").append(", ").append(bindings.getId()).append(", \"");
                    emitHumanDescription(builder, fieldBindings);
                    builder.append("\");\n");
                } else {
                    builder.append("view;\n");
                }
            }
        }
    }

    private void emitMethodBindings(StringBuilder builder, ViewBindings bindings) {
        Map<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> classMethodBindings = bindings.getMethodBindings();
        if (!classMethodBindings.isEmpty()) {
            String extraIndent = "";
            boolean needsNullChecked = bindings.getRequiredBindings().isEmpty();
            if (needsNullChecked) {
                builder.append("    if (view != null) {\n");
                extraIndent = "  ";
            }
            for (Entry<ListenerClass, Map<ListenerMethod, Set<MethodViewBinding>>> e : classMethodBindings.entrySet()) {
                int i;
                ListenerClass listener = (ListenerClass) e.getKey();
                Map<ListenerMethod, Set<MethodViewBinding>> methodBindings = (Map) e.getValue();
                boolean needsCast = !"android.view.View".equals(listener.targetType());
                builder.append(extraIndent).append("    ");
                if (needsCast) {
                    builder.append("((").append(listener.targetType());
                    if (listener.genericArguments() > 0) {
                        builder.append('<');
                        for (i = 0; i < listener.genericArguments(); i++) {
                            if (i > 0) {
                                builder.append(", ");
                            }
                            builder.append('?');
                        }
                        builder.append('>');
                    }
                    builder.append(") ");
                }
                builder.append("view");
                if (needsCast) {
                    builder.append(')');
                }
                builder.append(FilenameUtils.EXTENSION_SEPARATOR).append(listener.setter()).append("(\n");
                builder.append(extraIndent).append("      new ").append(listener.type()).append("() {\n");
                for (ListenerMethod method : getListenerMethods(listener)) {
                    builder.append(extraIndent).append("        @Override public ").append(method.returnType()).append(' ').append(method.name()).append("(\n");
                    String[] parameterTypes = method.parameters();
                    int count = parameterTypes.length;
                    for (i = 0; i < count; i++) {
                        builder.append(extraIndent).append("          ").append(parameterTypes[i]).append(" p").append(i);
                        if (i < count - 1) {
                            builder.append(',');
                        }
                        builder.append('\n');
                    }
                    builder.append(extraIndent).append("        ) {\n");
                    builder.append(extraIndent).append("          ");
                    boolean hasReturnType = !"void".equals(method.returnType());
                    if (hasReturnType) {
                        builder.append("return ");
                    }
                    if (methodBindings.containsKey(method)) {
                        Iterator<MethodViewBinding> iterator = ((Set) methodBindings.get(method)).iterator();
                        while (iterator.hasNext()) {
                            MethodViewBinding binding = (MethodViewBinding) iterator.next();
                            builder.append("target.").append(binding.getName()).append('(');
                            List<Parameter> parameters = binding.getParameters();
                            String[] listenerParameters = method.parameters();
                            count = parameters.size();
                            for (i = 0; i < count; i++) {
                                Parameter parameter = (Parameter) parameters.get(i);
                                int listenerPosition = parameter.getListenerPosition();
                                if (parameter.requiresCast(listenerParameters[listenerPosition])) {
                                    builder.append("finder.<").append(parameter.getType()).append(">castParam(p").append(listenerPosition).append(", \"").append(method.name()).append("\", ").append(listenerPosition).append(", \"").append(binding.getName()).append("\", ").append(i).append(")");
                                } else {
                                    builder.append('p').append(listenerPosition);
                                }
                                if (i < count - 1) {
                                    builder.append(", ");
                                }
                            }
                            builder.append(");");
                            if (iterator.hasNext()) {
                                builder.append("\n").append("          ");
                            }
                        }
                    } else if (hasReturnType) {
                        builder.append(method.defaultReturn()).append(';');
                    }
                    builder.append('\n');
                    builder.append(extraIndent).append("        }\n");
                }
                builder.append(extraIndent).append("      });\n");
            }
            if (needsNullChecked) {
                builder.append("    }\n");
            }
        }
    }

    static List<ListenerMethod> getListenerMethods(ListenerClass listener) {
        if (listener.method().length == 1) {
            return Arrays.asList(listener.method());
        }
        try {
            List<ListenerMethod> methods = new ArrayList();
            Class<? extends Enum<?>> callbacks = listener.callbacks();
            for (Enum<?> callbackMethod : (Enum[]) callbacks.getEnumConstants()) {
                ListenerMethod method = (ListenerMethod) callbacks.getField(callbackMethod.name()).getAnnotation(ListenerMethod.class);
                if (method == null) {
                    throw new IllegalStateException(String.format("@%s's %s.%s missing @%s annotation.", new Object[]{callbacks.getEnclosingClass().getSimpleName(), callbacks.getSimpleName(), callbackMethod.name(), ListenerMethod.class.getSimpleName()}));
                }
                methods.add(method);
            }
            return methods;
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private void emitUnbindMethod(StringBuilder builder) {
        builder.append("  @Override public void unbind(T target) {\n");
        if (this.parentViewBinder != null) {
            builder.append("    super.unbind(target);\n\n");
        }
        for (ViewBindings bindings : this.viewIdMap.values()) {
            for (FieldViewBinding fieldBinding : bindings.getFieldBindings()) {
                builder.append("    target.").append(fieldBinding.getName()).append(" = null;\n");
            }
        }
        for (FieldCollectionViewBinding fieldCollectionBinding : this.collectionBindings.keySet()) {
            builder.append("    target.").append(fieldCollectionBinding.getName()).append(" = null;\n");
        }
        builder.append("  }\n");
    }

    static void emitHumanDescription(StringBuilder builder, Collection<? extends ViewBinding> bindings) {
        Iterator<? extends ViewBinding> iterator = bindings.iterator();
        switch (bindings.size()) {
            case 1:
                builder.append(((ViewBinding) iterator.next()).getDescription());
                return;
            case 2:
                builder.append(((ViewBinding) iterator.next()).getDescription()).append(" and ").append(((ViewBinding) iterator.next()).getDescription());
                return;
            default:
                int count = bindings.size();
                for (int i = 0; i < count; i++) {
                    if (i != 0) {
                        builder.append(", ");
                    }
                    if (i == count - 1) {
                        builder.append("and ");
                    }
                    builder.append(((ViewBinding) iterator.next()).getDescription());
                }
                return;
        }
    }
}
