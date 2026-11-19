package com.releasebird.releasebird_sdk;

import static com.releasebird.releasebird_sdk.ReleasebirdUtils.convertDpToPixel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;


public class ReleasebirdButton {

    private ConstraintLayout layout;
    private TextView notificationCountTextView;
    private LinearLayout notificationContainerLayout;
    private LinearLayout notificationListContainer;
    private ImageButton imageButton;
    private Bitmap buttonIcon;
    private RelativeLayout closeButtonContainer;
    private RelativeLayout badgeContainer;
    private Button squareButton;
    private ConstraintLayout feedbackButtonRelativeLayout;
    private Handler handler;
    private Runnable runnable;

    private boolean enableButton;
    /** The host app hid the button explicitly – it stays hidden until the app shows it again. */
    private boolean hiddenByApp;
    private static final Handler MAIN = new Handler(android.os.Looper.getMainLooper());

    private TextView textView;

    private static ReleasebirdButton instance;

    private ReleasebirdButton() {

    }

    public static ReleasebirdButton getInstance() {
        if (instance == null) {
            instance = new ReleasebirdButton();
        }
        return instance;
    }

    public boolean attached = false;

    public static void animateViewInOut(View view, boolean show) {
        if (view == null) {
            return;
        }

        if (show && view.getVisibility() == View.VISIBLE) {
            return;
        }

        if (!show && (view.getVisibility() == View.GONE || view.getVisibility() == View.INVISIBLE)) {
            return;
        }

        view.setAlpha(show ? 0f : 1f);
        if (show) {
            view.setVisibility(View.VISIBLE);
        }

        ObjectAnimator fadeInAnimation = ObjectAnimator.ofFloat(view, "alpha", show ? 0f : 1f, show ? 1f : 0f);
        fadeInAnimation.setDuration(200);
        fadeInAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (!show && view != null) {
                    view.setVisibility(View.GONE);
                }
            }
        });
        fadeInAnimation.start();
    }

    public void addLayoutToActivity(Activity activity) {
        if (activity == null) {
            activity = ActivityUtil.getCurrentActivity();
        }

        if (activity == null) {
            return;
        }

        if (activity.getClass().getSimpleName().contains("Releasebird")) {
            return;
        }

        this.destroyUI();

        if (this.layout == null) {
            LayoutInflater inflater = activity.getLayoutInflater();
            this.layout = (ConstraintLayout) inflater.inflate(R.layout.activity_rbird_fab, null);
            this.layout.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            addLocalLayoutToActivity(activity);
        }

        addButton(activity);

        createNotificationLayout(activity);

        this.attached = true;
    }

    public void showButton(boolean showButton) {
        if (showButton && !enableButton) {
            return;
        }
        try {
            MAIN.post(new Runnable() {
                @Override
                public void run() {
                    boolean manualHidden = hiddenByApp;
                    if (!manualHidden) {
                        if (showButton) {
                            if (feedbackButtonRelativeLayout != null) {
                                feedbackButtonRelativeLayout.setVisibility(View.VISIBLE);

                                Activity currentActivity = ActivityUtil.getCurrentActivity();
                                if (currentActivity != null) {
                                    renderButton(currentActivity);
                                }
                            }
                        } else {
                            if (feedbackButtonRelativeLayout != null) {
                                feedbackButtonRelativeLayout.setVisibility(View.INVISIBLE);
                            }
                        }
                    } else {
                        if (feedbackButtonRelativeLayout != null) {
                            feedbackButtonRelativeLayout.setVisibility(View.INVISIBLE);
                        }
                    }
                }
            });
        } catch (Error | Exception ignore) {
        }
    }

    public void addLocalLayoutToActivity(Activity activity) {
        try {
            if (activity == null) {
                activity = ActivityUtil.getCurrentActivity();
            }
            if (activity == null) {
                return;
            }

            activity.addContentView(this.layout, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            layout.setFocusable(false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.layout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        int topPadding = insets.getSystemWindowInsetTop();
                        int bottomPadding = insets.getSystemWindowInsetBottom();
                        v.setPadding(0, topPadding, 0, bottomPadding);
                        return insets.consumeSystemWindowInsets();
                    }
                });
                this.layout.requestApplyInsets();
            }
        } catch (Error | Exception ignore) {
        }
    }

    public void destroyUI() {
        this.destroyButton();
        this.destroyNotificationLayout();
        this.destroyLayout();
    }

    public void destroyLayout() {
        if (this.layout != null) {
            this.layout.removeAllViews();
            this.layout.setOnApplyWindowInsetsListener(null);

            try {
                ViewParent parent = this.layout.getParent();
                if (parent != null) {
                    if (parent instanceof ViewGroup) {
                        ViewGroup viewGroupParent = (ViewGroup) parent;
                        viewGroupParent.removeView(this.layout);
                    }
                }
            } catch (Exception exp) {
            }
            this.layout = null;
        }
    }


    public void destroyButton() {
        if (this.squareButton != null) {
            this.squareButton.setOnClickListener(null);
            this.squareButton = null;
        }

        if (this.imageButton != null) {
            this.imageButton.setOnClickListener(null);
            this.imageButton.setImageDrawable(null);
            this.imageButton = null;
        }

        if (this.buttonIcon != null) {
            this.buttonIcon.recycle();
            this.buttonIcon = null;
        }

        if (this.notificationCountTextView != null) {
            this.notificationCountTextView = null;
        }

        if (this.feedbackButtonRelativeLayout != null) {
            this.feedbackButtonRelativeLayout.removeAllViews();

            try {
                if (this.layout != null) {
                    this.layout.removeView(this.feedbackButtonRelativeLayout);
                }
            } catch (Exception exp) {
            }

            this.feedbackButtonRelativeLayout = null;
        }
    }

    private void destroyNotificationLayout() {
        if (this.closeButtonContainer != null) {
            this.closeButtonContainer.removeAllViews();
            this.closeButtonContainer = null;
        }

        if (this.badgeContainer != null) {
            this.badgeContainer.removeAllViews();
            this.badgeContainer = null;
        }

        if (this.notificationListContainer != null) {
            this.notificationListContainer.removeAllViews();
            this.notificationListContainer = null;
        }

        if (this.notificationContainerLayout != null) {
            this.notificationContainerLayout.removeAllViews();
            this.notificationContainerLayout = null;
        }
    }


    public void addButton(Activity activity) {
        if (activity == null) {
            activity = ActivityUtil.getCurrentActivity();
        }

        if (activity == null) {
            return;
        }

        if (this.layout == null) {
            return;
        }

        if (feedbackButtonRelativeLayout != null) {
            return;
        }

        String screenName = activity.getClass().getSimpleName();
        if (screenName.equals("ReleasebirdMainActivity")) {
            return;
        }

        try {
            if (feedbackButtonRelativeLayout == null) {
                feedbackButtonRelativeLayout = new ConstraintLayout(activity);
            }

            Activity finalActivity = activity;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    feedbackButtonRelativeLayout.setId(View.generateViewId());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    feedbackButtonRelativeLayout.setLayoutParams(params);
                    feedbackButtonRelativeLayout.setVisibility(View.INVISIBLE);
                    layout.addView(feedbackButtonRelativeLayout);
                    renderButton(finalActivity);

                }
            });
        } catch (Exception ex) {
        }
    }

    private void renderButton(Activity local) {
        try {
            if (!enableButton) {
                return;
            }
            if (imageButton == null) {
                imageButton = new ImageButton(local);
                imageButton.setId(View.generateViewId());

                GradientDrawable gdDefault = new GradientDrawable();
                gdDefault.setColor(launcherColor());
                gdDefault.setCornerRadius(1000);

                imageButton.setBackground(gdDefault);
                imageButton.setAdjustViewBounds(true);
                imageButton.setScaleType(ImageView.ScaleType.FIT_CENTER);
                imageButton.setVisibility(View.INVISIBLE);

                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        setInvisible();
                        ReleasebirdActivityManager.getInstance().showFrame(local);
                    }
                });
            }

            boolean manualHidden = hiddenByApp;
            if (!manualHidden) {
                feedbackButtonRelativeLayout.setVisibility(View.VISIBLE);
            } else {
                feedbackButtonRelativeLayout.setVisibility(View.GONE);
            }

            if (feedbackButtonRelativeLayout.indexOfChild(imageButton) < 0) {
                feedbackButtonRelativeLayout.addView(imageButton, convertDpToPixel(54, local), convertDpToPixel(54, local));
            }

            GradientDrawable gdDefaultText = new GradientDrawable();
            gdDefaultText.setColor(Color.RED);

            gdDefaultText.setCornerRadius(1000);

            notificationCountTextView = new TextView(local);
            notificationCountTextView.setId(View.generateViewId());
            notificationCountTextView.setBackground(gdDefaultText);
            notificationCountTextView.setTextColor(Color.WHITE);
            notificationCountTextView.setTextSize(12);
            notificationCountTextView.setText(String.valueOf(1));
            notificationCountTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            notificationCountTextView.setGravity(Gravity.CENTER);
            notificationCountTextView.setVisibility(View.GONE);
            notificationCountTextView.bringToFront();
            feedbackButtonRelativeLayout.addView(notificationCountTextView, convertDpToPixel(18, local), convertDpToPixel(18, local));

            if (buttonIcon == null) {
                new ReleasebirdRoundImageHandler(ReleasebirdConfig.getInstance().getChatBubble(), imageButton, new ReleasebirdImageLoaded() {
                    @Override
                    public void invoke(Bitmap bitmap) {
                        buttonIcon = bitmap;

                        local.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ReleasebirdButton.animateViewInOut(imageButton, true);
                            }
                        });
                    }
                }).execute();
            } else {
                imageButton.setImageBitmap(buttonIcon);
                imageButton.setVisibility(View.VISIBLE);
            }

            int offsetX = ReleasebirdConfig.getInstance().getWidgetX();
            int offsetY = ReleasebirdConfig.getInstance().getWidgetY();

            ConstraintSet set = new ConstraintSet();
            set.clone(layout);
            // launcher position from the widget settings: right (default) or left
            boolean left = "left".equalsIgnoreCase(ReleasebirdConfig.getInstance().getLauncherPosition());
            set.connect(feedbackButtonRelativeLayout.getId(), left ? ConstraintSet.START : ConstraintSet.END, layout.getId(),
                    left ? ConstraintSet.START : ConstraintSet.END, convertDpToPixel(Math.max(offsetX - 20, 0), local));
            set.connect(feedbackButtonRelativeLayout.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, convertDpToPixel(offsetY, local));
            set.applyTo(layout);
        } catch (Exception ex) {
        }
    }

    /** Launcher colour from the widget settings; falls back to the brand colour if it is missing or malformed. */
    private static int launcherColor() {
        try {
            String color = ReleasebirdConfig.getInstance().getLauncherColor();
            if (color != null && !color.isEmpty() && !"null".equals(color)) {
                return Color.parseColor(color);
            }
        } catch (Exception ignore) {
        }
        return Color.parseColor("#667eea");
    }

    public void changeBadge(final String value) {
        // Called from network threads – always hop to the main thread, never rely on an activity being there
        MAIN.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (textView == null || badgeContainer == null) {
                        return;
                    }
                    textView.setText(value);
                    badgeContainer.setVisibility("0".equals(value) ? View.INVISIBLE : View.VISIBLE);
                } catch (Exception ignore) {
                }
            }
        });
    }

    public void createNotificationLayout(Activity activity) {
        if (activity == null) {
            activity = ActivityUtil.getCurrentActivity();
        }

        if (activity == null) {
            return;
        }

        if (this.layout == null) {
            return;
        }

        Activity finalActivity = activity;
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    notificationContainerLayout = new LinearLayout(finalActivity);
                    notificationContainerLayout.setId(View.generateViewId());
                    notificationContainerLayout.setOrientation(LinearLayout.VERTICAL);
                    notificationContainerLayout.setGravity(Gravity.LEFT);

                    int offsetX = 50;
                    int offsetY = 50;

                    layout.addView(notificationContainerLayout);

                    ConstraintSet set = new ConstraintSet();
                    set.clone(layout);

                    int viewPadding = 20;

                    boolean manualHidden = false;
                    boolean canShowFeedbackButton = !manualHidden;

                    if (feedbackButtonRelativeLayout == null || !canShowFeedbackButton) {
                        set.connect(notificationContainerLayout.getId(), ConstraintSet.BOTTOM, layout.getId(), ConstraintSet.BOTTOM, convertDpToPixel(20, finalActivity));
                        set.connect(notificationContainerLayout.getId(), ConstraintSet.START, layout.getId(), ConstraintSet.START, convertDpToPixel(0, finalActivity));
                    } else {
                        set.connect(notificationContainerLayout.getId(), ConstraintSet.BOTTOM, feedbackButtonRelativeLayout.getId(), ConstraintSet.TOP, convertDpToPixel(15, finalActivity));
                        set.connect(notificationContainerLayout.getId(), ConstraintSet.END, layout.getId(), ConstraintSet.END, convertDpToPixel(offsetX - 20, finalActivity));
                        viewPadding = offsetX;
                        notificationContainerLayout.setGravity(Gravity.RIGHT);
                    }

                    try {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        finalActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        int deviceWidth = displayMetrics.widthPixels;
                        int deviceHeight = displayMetrics.heightPixels;
                        int smallerDimension = Math.min(deviceWidth, deviceHeight);
                        int maxWidthPx = smallerDimension - convertDpToPixel(viewPadding * 2, finalActivity);
                        set.constrainMaxWidth(notificationContainerLayout.getId(), maxWidthPx);
                    } catch (Exception exp) {
                    }

                    set.applyTo(layout);

                    if (badgeContainer == null) {
                        GradientDrawable gradientDrawable = new GradientDrawable();
                        gradientDrawable.setCornerRadius(1000);
                        gradientDrawable.setColor(Color.parseColor("#ff0021"));

                        badgeContainer = new RelativeLayout(finalActivity.getApplication().getApplicationContext());
                        badgeContainer.setGravity(Gravity.RIGHT);
                        badgeContainer.setId(View.generateViewId());
                        badgeContainer.setVisibility(View.INVISIBLE);

                        RelativeLayout view = new RelativeLayout(finalActivity.getApplication().getApplicationContext());
                        view.setBackground(gradientDrawable);
                        view.setMinimumWidth(50);
                        view.setMinimumHeight(50);

                        textView = new TextView(finalActivity.getApplication().getApplicationContext());
                        textView.setId(View.generateViewId());
                        textView.setTextColor(Color.parseColor("#FFFFFF"));
                        textView.setTextSize(12);
                        view.addView(textView);
                        view.setPadding(15, 15, 15, 15);

                        badgeContainer.addView(view);
                        feedbackButtonRelativeLayout.addView(badgeContainer);
                        ConstraintSet set2 = new ConstraintSet();
                        set2.clone(feedbackButtonRelativeLayout);
                        set2.connect(badgeContainer.getId(), ConstraintSet.START, feedbackButtonRelativeLayout.getId(), ConstraintSet.START, convertDpToPixel(40, finalActivity));
                        set2.connect(badgeContainer.getId(), ConstraintSet.TOP, feedbackButtonRelativeLayout.getId(), ConstraintSet.TOP, convertDpToPixel(0, finalActivity));
                        set2.applyTo(feedbackButtonRelativeLayout);


                    }

                    if (closeButtonContainer == null) {
                        ImageButton closeButton = new ImageButton(finalActivity.getApplication().getApplicationContext());

                        GradientDrawable gradientDrawable = new GradientDrawable();
                        gradientDrawable.setCornerRadius(1000);
                        gradientDrawable.setColor(Color.parseColor("#878787"));

                        closeButtonContainer = new RelativeLayout(finalActivity.getApplication().getApplicationContext());
                        closeButtonContainer.setGravity(Gravity.RIGHT);

                        LinearLayout.LayoutParams closeContainerParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                        closeContainerParams.setMargins(convertDpToPixel(20, finalActivity), convertDpToPixel(0, finalActivity), convertDpToPixel(20, finalActivity), 0);
                        closeButtonContainer.setLayoutParams(closeContainerParams);

                        closeButton.setBackgroundResource(R.drawable.close_white);
                        closeButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (notificationListContainer != null) {
                                    notificationListContainer.removeAllViews();
                                }
                                if (closeButtonContainer != null) {
                                    closeButtonContainer.setVisibility(View.GONE);
                                }
                            }
                        });

                        RelativeLayout view = new RelativeLayout(finalActivity.getApplication().getApplicationContext());
                        view.addView(closeButton, convertDpToPixel(18, finalActivity), convertDpToPixel(18, finalActivity));
                        view.setBackground(gradientDrawable);
                        view.setPadding(15, 15, 15, 15);
                        closeButtonContainer.setVisibility(View.GONE);
                        closeButtonContainer.addView(view);
                        notificationContainerLayout.addView(closeButtonContainer);
                    }

                    if (notificationListContainer == null) {
                        notificationListContainer = new LinearLayout(finalActivity.getApplication().getApplicationContext());
                        notificationListContainer.setOrientation(LinearLayout.VERTICAL);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            notificationListContainer.setGravity(notificationContainerLayout.getGravity());
                        }
                        notificationContainerLayout.addView(notificationListContainer);
                    }

                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        });
    }

    public boolean isEnableButton() {
        return enableButton;
    }

    public void setEnableButton(boolean enableButton) {
        this.enableButton = enableButton;
    }

    public void setHiddenByApp(boolean hiddenByApp) {
        this.hiddenByApp = hiddenByApp;
    }

    public void setVisible() {
        if (hiddenByApp) {
            return;
        }
        if (feedbackButtonRelativeLayout != null) {
            feedbackButtonRelativeLayout.setVisibility(View.VISIBLE);
        }
    }

    public void setInvisible() {
        if (feedbackButtonRelativeLayout != null) {
            feedbackButtonRelativeLayout.setVisibility(View.INVISIBLE);
        }
    }


}
