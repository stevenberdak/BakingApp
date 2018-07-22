package com.sbschoolcode.bakingapp.ui.recipe;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.sbschoolcode.bakingapp.AppConstants;
import com.sbschoolcode.bakingapp.R;
import com.sbschoolcode.bakingapp.controllers.ServiceController;
import com.sbschoolcode.bakingapp.models.Recipe;
import com.sbschoolcode.bakingapp.ui.recipe.steps.SelectStepFrag;
import com.sbschoolcode.bakingapp.widget.BakingWidget;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecipeActivity extends AppCompatActivity {

    public static final String ACTION_RECIPE_QUERIED = "com.sbschoolcode.broadcast.RECIPE_QUERIED";
    @BindView(R.id.content_frame)
    FrameLayout mContentFrame;
    private BroadcastReceiver mRecipeReceiver;
    private IntentFilter mRecipeIntentFilter;
    private Bundle mCurrentBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);
        ButterKnife.bind(this);
        initReceiver();

        if (savedInstanceState == null)
            ServiceController.getInstance().startBuildRecipeItem(this, getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentBundle != null) outState.putAll(mCurrentBundle);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentBundle = new Bundle();
        mCurrentBundle.putParcelable(AppConstants.INTENT_EXTRA_RECIPE,
                savedInstanceState.getParcelable(AppConstants.INTENT_EXTRA_RECIPE));
        mCurrentBundle.putParcelableArrayList(AppConstants.INTENT_EXTRA_INGREDIENTS_LIST,
                savedInstanceState.getParcelableArrayList(AppConstants.INTENT_EXTRA_INGREDIENTS_LIST));
        mCurrentBundle.putParcelableArrayList(AppConstants.INTENT_EXTRA_STEPS_LIST,
                savedInstanceState.getParcelableArrayList(AppConstants.INTENT_EXTRA_STEPS_LIST));
    }

    private void loadSteps() {
        Recipe recipe = mCurrentBundle.getParcelable(AppConstants.INTENT_EXTRA_RECIPE);
        if (recipe != null) {
            setTitle(recipe.name);
            updateWidget(recipe.name);
        }

        SelectStepFrag fragment = new SelectStepFrag();
        fragment.setArguments(mCurrentBundle);
        getSupportFragmentManager().beginTransaction()
                .replace(mContentFrame.getId(), fragment, AppConstants.FRAGMENT_SELECT_A_STEP_TAG).commit();
    }

    private void updateWidget(String name) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(AppConstants.PREF_DETAILS_RECIPE_NAME, name).apply();

        Intent updateWidgets = new Intent(this, BakingWidget.class);
        updateWidgets.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] widgetIds = AppWidgetManager.getInstance(getApplication()).getAppWidgetIds(new ComponentName(getApplication(), BakingWidget.class));
        Log.v(AppConstants.TESTING, "Widget id's = " + Arrays.toString(widgetIds));
        updateWidgets.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        sendBroadcast(updateWidgets);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mRecipeReceiver, mRecipeIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mRecipeReceiver);
    }

    @Override
    public boolean onSupportNavigateUp() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(AppConstants.PREF_DETAILS_LOADED, -1).apply();
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return true;
        } else {
            return super.onSupportNavigateUp();
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putInt(AppConstants.PREF_DETAILS_LOADED, -1).apply();
        super.onBackPressed();
    }

    private void initReceiver() {
        mRecipeReceiver = new RecipeReceiver();
        mRecipeIntentFilter = new IntentFilter();
        mRecipeIntentFilter.addAction(ACTION_RECIPE_QUERIED);
    }

    private void onError() {
        Toast.makeText(RecipeActivity.this, R.string.error_loading_recipe, Toast.LENGTH_LONG).show();
        Log.v(AppConstants.TESTING, "onError()");
        finish();
    }

    private class RecipeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_RECIPE_QUERIED)) {
                Recipe recipe = intent.getParcelableExtra(AppConstants.INTENT_EXTRA_RECIPE);
                if (recipe == null) onError();
                else {
                    Log.v(AppConstants.TESTING, "Recipe received, recipe name = " + recipe);
                    mCurrentBundle = intent.getExtras();
                    loadSteps();
                }
            }
        }
    }
}
