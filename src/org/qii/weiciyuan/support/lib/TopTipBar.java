package org.qii.weiciyuan.support.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;
import org.qii.weiciyuan.R;
import org.qii.weiciyuan.bean.ItemBean;
import org.qii.weiciyuan.bean.ListBean;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * User: qii
 * Date: 13-4-20
 */
public class TopTipBar extends TextView {

    public enum Type {
        ALWAYS, AUTO
    }

    private HashSet<String> ids = new HashSet<String>();
    private boolean disappear = false;
    private Runnable lastRunnable;
    private boolean error;
    private OnChangeListener onChangeListener;
    private Type type;


    public static interface OnChangeListener {
        public void onChange(int count);

    }

    public void setOnChangeListener(OnChangeListener l) {
        this.onChangeListener = l;
        this.onChangeListener.onChange(ids.size());

    }

    public TopTipBar(Context context) {
        this(context, null);
    }

    public TopTipBar(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public TopTipBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        type = Type.AUTO;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public HashSet<String> getValues() {
        HashSet<String> copy = new HashSet<String>();
        copy.addAll(this.ids);
        return copy;
    }

    public void setValue(HashSet<String> values) {
        this.ids.clear();
        this.ids.addAll(values);
        this.disappear = false;
        setCount();
    }

    public void setValue(ListBean<?, ?> listData, boolean disappear) {
        this.disappear = disappear;
        List<? extends ItemBean> values = listData.getItemList();
        for (ItemBean b : values) {
            if (b != null)
                ids.add(b.getId());
        }
        setCount();
        if (disappear) {
            disappear(3000);
            ids.clear();
        }

        if (this.onChangeListener != null) {
            this.onChangeListener.onChange(ids.size());
        }
    }

    private void disappear(int duration) {
        if (getVisibility() == View.INVISIBLE || getVisibility() == View.GONE) {
            return;
        }
        if (lastRunnable != null) {
            getHandler().removeCallbacks(lastRunnable);
        }
        lastRunnable = new Runnable() {
            @Override
            public void run() {
                animate().alpha(0).setDuration(300).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        setVisibility(View.INVISIBLE);
                        animate().alpha(1.0f).setListener(null);
                        if (ids.size() > 0) {
                            setCount();
                        }
                    }
                });
            }

        };
        Handler handler = getHandler();
        if (handler != null)
            handler.postDelayed(lastRunnable, duration);
    }


    private void setCount() {

        this.error = false;
        int count = ids.size();
        if (count > 0) {
            setVisibility(View.VISIBLE);
            setText(String.format(getContext().getString(R.string.new_messages_count), String.valueOf(ids.size())));
            setBackgroundResource(R.color.top_tip_bar_tip);
        } else {
            disappear(0);
        }
    }


    public void hideCount() {
        if (!error) {
            setVisibility(View.INVISIBLE);
        }
    }

    //helperId can be used to keep TopTipBar stay Visible status
    public void handle(String id, String helperId) {
        if (disappear) {
            return;
        }
        boolean has = ids.contains(id);
        if (has) {
            ids.remove(id);
            long d = Long.valueOf(id);
            Iterator<String> iterator = ids.iterator();
            while (iterator.hasNext()) {
                String v = iterator.next();
                long t = Long.valueOf(v);
                if (t < d) {
                    iterator.remove();
                }
            }
            setCount();

            if (this.onChangeListener != null) {
                this.onChangeListener.onChange(ids.size());
            }
        }

        if (type == Type.ALWAYS) {
            return;
        }

        if (ids.contains(helperId)) {
            setCount();
        } else {
            setVisibility(View.INVISIBLE);
        }

    }

    public void clearAndReset() {
        if (disappear) {
            return;
        }
        ids.clear();
        disappear(0);
        if (this.onChangeListener != null) {
            this.onChangeListener.onChange(ids.size());
        }
    }

    public void setError(String error) {
        this.disappear = true;
        this.error = true;
        setVisibility(View.VISIBLE);
        animate().alpha(1.0f);
        setText(error);
        disappear(3000);
        setBackgroundResource(R.color.top_tip_bar_error);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        //begin boilerplate code that allows parent classes to save state
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        //end

        ss.ids = this.ids;
        ss.disappear = this.disappear;
        ss.visible = this.isShown();
        ss.type = this.type;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.ids = ss.ids;
        this.disappear = ss.disappear;
        this.type = ss.type;
        if (ss.visible) {
            setVisibility(View.VISIBLE);
        }
    }

    static class SavedState extends BaseSavedState {
        HashSet<String> ids;
        boolean disappear;
        boolean visible;
        Type type;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            Bundle bundle = in.readBundle();
            this.ids = (HashSet<String>) bundle.getSerializable("ids");
            this.type = (Type) bundle.getSerializable("type");
            boolean[] disappearArray = new boolean[2];
            in.readBooleanArray(disappearArray);
            this.disappear = disappearArray[0];
            this.visible = disappearArray[1];
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            Bundle bundle = new Bundle();
            bundle.putSerializable("ids", ids);
            bundle.putSerializable("type", type);
            out.writeBundle(bundle);
            out.writeBooleanArray(new boolean[]{this.disappear, this.visible});
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }

                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
