package com.fanyu.boundless.widget;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.fanyu.boundless.R;

public class Exsit extends Dialog {

    @SuppressLint({"WrongViewCast"})
    public static class Builder {
        private View contentView;
        private Context context;
        private String message;
        private OnClickListener negativeButtonClickListener;
        private String negativeButtonText;
        private OnClickListener positiveButtonClickListener;
        private String positiveButtonText;
        private String title;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setMessage(int message) {
            this.message = (String) this.context.getText(message);
            return this;
        }

        public Builder setTitle(int title) {
            this.title = (String) this.context.getText(title);
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }

        public Builder setPositiveButton(int positiveButtonText, OnClickListener listener) {
            this.positiveButtonText = (String) this.context.getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(String positiveButtonText, OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText, OnClickListener listener) {
            this.negativeButtonText = (String) this.context.getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(String negativeButtonText, OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Exsit create() {
            LayoutInflater inflater = (LayoutInflater) this.context.getSystemService("layout_inflater");
            final Exsit dialog = new Exsit(this.context, R.style.Dialog);
            View layout = inflater.inflate(R.layout.exsit, null);
            dialog.addContentView(layout, new LayoutParams(-1, -2));
            ((TextView) layout.findViewById(R.id.title)).setText(this.title);
            if (this.positiveButtonText != null) {
                ((Button) layout.findViewById(R.id.positiveButton)).setText(this.positiveButtonText);
                if (this.positiveButtonClickListener != null) {
                    ((Button) layout.findViewById(R.id.positiveButton)).setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Builder.this.positiveButtonClickListener.onClick(dialog, -1);
                        }
                    });
                }
            } else {
                layout.findViewById(R.id.positiveButton).setVisibility(8);
            }
            if (this.negativeButtonText != null) {
                ((Button) layout.findViewById(R.id.negativeButton)).setText(this.negativeButtonText);
                if (this.negativeButtonClickListener != null) {
                    ((Button) layout.findViewById(R.id.negativeButton)).setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Builder.this.negativeButtonClickListener.onClick(dialog, -2);
                        }
                    });
                }
            } else {
                layout.findViewById(R.id.negativeButton).setVisibility(8);
            }
            if (this.message != null) {
                ((TextView) layout.findViewById(R.id.message)).setText(this.message);
            } else if (this.contentView != null) {
                ((LinearLayout) layout.findViewById(R.id.message)).removeAllViews();
                ((LinearLayout) layout.findViewById(R.id.message)).addView(this.contentView, new LayoutParams(-2, -2));
            }
            dialog.setContentView(layout);
            return dialog;
        }
    }

    public Exsit(Context context) {
        super(context);
    }

    public Exsit(Context context, int theme) {
        super(context, theme);
    }
}
