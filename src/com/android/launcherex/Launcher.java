/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcherex;

import static android.util.Log.d;
import static android.util.Log.e;
import static android.util.Log.w;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import com.android.launcherex.ScreenLayout.onScreenChangeListener;
import mobi.intuitit.android.content.LauncherIntent;
import mobi.intuitit.android.content.LauncherMetadata;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.Intent.ShortcutIconResource;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.LiveFolders;
import android.provider.Settings;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.adw.launcher.ResizeViewHandler;
import org.adw.launcher.AllAppsGridView;

/**
 * Drop it like its hot
 */
public final class Launcher extends Activity implements View.OnClickListener, OnLongClickListener, OnSharedPreferenceChangeListener {
	
    static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";
    static final String LOG_TAG = "Launcher";
    static final int APPWIDGET_HOST_ID = 1024;
    static final int NUMBER_CELLS_X = 4;
    static final int NUMBER_CELLS_Y = 4;
    static final int DEFAULT_SCREEN = 2;
    static final int SCREEN_COUNT = 5;
    static final boolean LOGD = false;
    
    public static boolean hideLabels;
    public static boolean enableScreenBounce;
    
    public static final String PREFERENCES = "launcher.preferences";
    public static final String STYLE_PREF = "launcher.preferences.style";
    public static final String PREFERENCE_STYLE = "style_preference_int2";
    
    private static final boolean oldSchool = false;
    private static final boolean PROFILE_STARTUP = false;
    private static final boolean PROFILE_ROTATE = false;
    private static final boolean DEBUG_USER_INTERFACE = false;
    private static final int MENU_GROUP_ADD = 1;
    private static final int MENU_ADD = Menu.FIRST + 1;
    private static final int MENU_WALLPAPER_SETTINGS = MENU_ADD + 1;
    private static final int MENU_SEARCH = MENU_WALLPAPER_SETTINGS + 1;
    private static final int MENU_PREFERENCES = MENU_SEARCH + 1;
    private static final int MENU_SETTINGS = MENU_PREFERENCES + 1;
    private static final int REQUEST_CREATE_SHORTCUT = 1;
    private static final int REQUEST_CREATE_LIVE_FOLDER = 4;
    private static final int REQUEST_CREATE_APPWIDGET = 5;
    private static final int REQUEST_PICK_APPLICATION = 6;
    private static final int REQUEST_PICK_SHORTCUT = 7;
    private static final int REQUEST_PICK_LIVE_FOLDER = 8;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int WALLPAPER_SCREENS_SPAN = 2;
    private static final int DIALOG_CREATE_SHORTCUT = 1;
    private static final int DIALOG_RENAME_FOLDER = 2;

    private static final Object sLock = new Object();
    private static final String SEARCH_WIDGET = "search_widget";
    private static final LauncherModel sModel = new LauncherModel();
    private static final String EXTRA_CUSTOM_WIDGET = "custom_widget";
    private static final String RUNTIME_STATE_USER_FOLDERS = "launcher.user_folder";
    private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";
    private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cellX";
    private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cellY";
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_spanX";
    private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_spanY";
    private static final String RUNTIME_STATE_PENDING_ADD_COUNT_X = "launcher.add_countX";
    private static final String RUNTIME_STATE_PENDING_ADD_COUNT_Y = "launcher.add_countY";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";
    private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";
    private static final String RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS = "launcher.add_occupied_cells";

    private final int GENERIC = 0;
    private final int OUT_OF_SPACE = 1;
    private final int NOT_SUPPORTED = 2;
    private final int ACTIVITY_NOT_FOUND = 3;
    private static int sScreen = DEFAULT_SCREEN;
    private final int[] mCellCoordinates = new int[2];
    private final ContentObserver mObserver = new FavoritesChangeObserver();
    private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();
    private final BroadcastReceiver mApplicationsReceiver = new ApplicationsIntentReceiver();
    private final BroadcastReceiver mCloseSystemDialogsReceiver = new CloseSystemDialogsIntentReceiver();

    private boolean mRestoring;
    private boolean mDestroyed;
    private boolean mIsNewIntent;
    private boolean mLocaleChanged;
    private boolean keepIndicatorOn;
    private boolean keepIndicatorOff;
    private boolean mWaitingForResult;
    private boolean mIsEditMode = false;
    private boolean mDesktopLocked = true;
	private boolean mIsWidgetEditMode = false;
    private DragLayer mDragLayer;
    private Workspace mWorkspace;
    private LayoutInflater mInflater;
    private AppWidgetManager mAppWidgetManager;
    private LauncherAppWidgetHost mAppWidgetHost;

    private Bundle mSavedState;
    private DesktopBinder mBinder;
    private FolderInfo mFolderInfo;
    private Bundle mSavedInstanceState;
    private AllAppsGridView mAllAppsGrid;
    private CellLayout.CellInfo mMenuAddInfo;
    private CellLayout.CellInfo mAddItemCellInfo;
    private SpannableStringBuilder mDefaultKeySsb = null;
    private LauncherAppWidgetInfo mlauncherAppWidgetInfo=null;

    private View mScreensEditor=null;
    private RelativeLayout mFroyo;
    private RelativeLayout mGingerbread;
    private RelativeLayout mSense;
    private RelativeLayout mSlate;
    private RelativeLayout mMusic;
    private RelativeLayout mBlackboard;
    private RelativeLayout mEspresso;
    private RelativeLayout mBlur;
    private RelativeLayout mNinja;
    private RelativeLayout mSamurai;
    private RelativeLayout mPro;
    private AnimatedDrawer mDeleteZoneRelative;
    private AnimatedDrawer mAnimatedDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mInflater = getLayoutInflater();

        mAppWidgetManager = AppWidgetManager.getInstance(this);

        mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
        mAppWidgetHost.startListening();

        if (PROFILE_STARTUP) {
            android.os.Debug.startMethodTracing("/sdcard/launcher");
        }

        checkForLocaleChange();
        setWallpaperDimension();

        setContentView(R.layout.launcher);
        setupViews();

        registerIntentReceivers();
        registerContentObservers();

        mSavedState = savedInstanceState;
        restoreState(mSavedState);

        if (PROFILE_STARTUP) {
            android.os.Debug.stopMethodTracing();
        }

        if (!mRestoring)
            startLoaders();

        // For handling default keys
        mDefaultKeySsb = new SpannableStringBuilder();
        Selection.setSelection(mDefaultKeySsb, 0);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        
        if (mAnimatedDrawer.getVisibility() == View.VISIBLE)
        	AnimatedDrawerClose(true);
        
        styleSelect();
        loadPreferences();
        registerIntentReceivers();
        	
        keepIndicatorOn = false;
        mIndicator.setVisibility(keepIndicatorOn ? View.VISIBLE : View.GONE);

        if (mRestoring)
            startLoaders();

        // If this was a new intent (i.e., the mIsNewIntent flag got set to true by
        // onNewIntent), then close the search dialog if needed, because it probably
        // came from the user pressing 'home' (rather than, for example, pressing 'back').
        if (mIsNewIntent) {
            // Post to a handler so that this happens after the search dialog tries to open
            // itself again.
            mWorkspace.post(new Runnable() {
                public void run() {
                    SearchManager searchmanager = (SearchManager) Launcher.this
                            .getSystemService(Context.SEARCH_SERVICE);
                    try {
                        searchmanager.stopSearch();
                    } catch (Exception e) {
                        e(LOG_TAG, "error stopping search", e);
                    }
                }
            });
        }

        mIsNewIntent = false;
    }
    
    private Uri getDefaultBrowserUri() {
    	String url = getString(R.string.default_browser_url);
    	if (url.indexOf("{CID}") != -1)
    		url = url.replace("{CID}", "android-google");
    	return Uri.parse(url);
	}
 
    public void onButtonDialer(View view) {
    	startActivity(new Intent(Intent.ACTION_DIAL));
    }

    public void onButtonBrowser(View view) {
    	String defaultUri = getString(R.string.default_browser_url);
    	startActivity(new Intent(Intent.ACTION_VIEW, ((defaultUri != null) ? Uri.parse(defaultUri) : getDefaultBrowserUri())).addCategory(Intent.CATEGORY_BROWSABLE));
    }
    
    public void onButtonSMS(View view) {
    	try {
    		startActivity(new Intent().setClassName("com.android.mms", "com.android.mms.ui.ConversationList"));
		} catch (ActivityNotFoundException e){
			makeToast(NOT_SUPPORTED);
		} catch (SecurityException e){
			makeToast(NOT_SUPPORTED);
		}
    }

    public void onButtonContacts(View view) {
    	try { 
    		startActivity(new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)); 
    	} catch (ActivityNotFoundException e){
    		makeToast(NOT_SUPPORTED);
    	}
    }
    
    public void onButtonAddWidget(View view) {
    	addItems();
    }
 
    public void onButtonDrawerOpen(View view) {
    	AnimatedDrawerOpen(true);
    }
    
    public void windowDismiss(View view) {
    	AnimatedDrawerClose(true);
    }
 
    private void AnimatedDrawerOpen(boolean animation) {
    	mAnimatedDrawer.open(animation);
    }
    
    public void AnimatedDrawerClose(boolean animation) {
    	mAnimatedDrawer.close(animation);
    }
    
    public void makeToast() {
    	makeToast(GENERIC);
    }
    
    private void makeToast(String string) {
    	Toast.makeText(Launcher.this, string, Toast.LENGTH_SHORT).show();
    }
    
    private void makeToast(int type) {
    	switch(type){
    		case NOT_SUPPORTED:
    			Toast.makeText(Launcher.this, "Sorry, your phone is not supported", Toast.LENGTH_SHORT).show();
    			break;
    		case GENERIC:
    			Toast.makeText(Launcher.this, GENERIC(), Toast.LENGTH_SHORT).show();
    			break;
    		case OUT_OF_SPACE:
    			Toast.makeText(this, getString(R.string.out_of_space), Toast.LENGTH_SHORT).show();
    			break;
    		case ACTIVITY_NOT_FOUND:
    			Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
    			break;
    	}
    }
    
    private String GENERIC() {
    	switch(new Random().nextInt(10)) {
    		case 0:
    			return "This is not the droid your looking for";
    		case 1:
    			return "Your holding it wrong";
    		case 2:
    			return "Oh snap";
    		case 3:
    			return "Oops";
    		case 4:
    			return "I didn't do it";
    		case 5:
    			return "I guess this means it didn't work?";
    		case 6:
    			return "Something went wrong, I blame you";
    		case 7:
    			return "o.O? What are YOU doing?";
    		case 8:
    			return "Houston we have a problem";
    		case 9:
    			return "Boink";
    		default :
    			return "Generic funny error";
    	}
    }
 
    private void checkForLocaleChange() {
        final LocaleConfiguration localeConfiguration = new LocaleConfiguration();
        readConfiguration(this, localeConfiguration);

        final Configuration configuration = getResources().getConfiguration();

        final String previousLocale = localeConfiguration.locale;
        final String locale = configuration.locale.toString();

        final int previousMcc = localeConfiguration.mcc;
        final int mcc = configuration.mcc;

        final int previousMnc = localeConfiguration.mnc;
        final int mnc = configuration.mnc;

        mLocaleChanged = !locale.equals(previousLocale) || mcc != previousMcc || mnc != previousMnc;

        if (mLocaleChanged) {
            localeConfiguration.locale = locale;
            localeConfiguration.mcc = mcc;
            localeConfiguration.mnc = mnc;

            writeConfiguration(this, localeConfiguration);
        }
    }

    private static class LocaleConfiguration {
        public String locale;
        public int mcc = -1;
        public int mnc = -1;
    }

    private static void readConfiguration(Context context, LocaleConfiguration configuration) {
        DataInputStream in = null;
        try {
            in = new DataInputStream(context.openFileInput(PREFERENCES));
            configuration.locale = in.readUTF();
            configuration.mcc = in.readInt();
            configuration.mnc = in.readInt();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // Ignore
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    private static void writeConfiguration(Context context, LocaleConfiguration configuration) {
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(context.openFileOutput(PREFERENCES, MODE_PRIVATE));
            out.writeUTF(configuration.locale);
            out.writeInt(configuration.mcc);
            out.writeInt(configuration.mnc);
            out.flush();
        } catch (FileNotFoundException e) {
            // Ignore
        } catch (IOException e) {
            // noinspection ResultOfMethodCallIgnored
            context.getFileStreamPath(PREFERENCES).delete();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    static int getScreen() {
        synchronized (sLock) {
            return sScreen;
        }
    }

    static void setScreen(int screen) {
        synchronized (sLock) {
            sScreen = screen;
        }
    }

    private void startLoaders() {
        boolean loadApplications = sModel.loadApplications(true, this, mLocaleChanged);
        sModel.loadUserItems(!mLocaleChanged, this, mLocaleChanged, loadApplications);

        mRestoring = false;
    }

    private void setWallpaperDimension() {
        WallpaperManager wpm = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);

        Display display = getWindowManager().getDefaultDisplay();
        boolean isPortrait = display.getWidth() < display.getHeight();

        final int width = isPortrait ? display.getWidth() : display.getHeight();
        final int height = isPortrait ? display.getHeight() : display.getWidth();
        wpm.suggestDesiredDimensions(width * WALLPAPER_SCREENS_SPAN, height);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mWaitingForResult = false;

        // The pattern used here is that a user PICKs a specific application,
        // which, depending on the target, might need to CREATE the actual target.

        // For example, the user would PICK_SHORTCUT for "Music playlist", and we
        // launch over to the Music app to actually CREATE_SHORTCUT.

        if (resultCode == RESULT_OK && mAddItemCellInfo != null) {
            switch (requestCode) {
            case REQUEST_PICK_APPLICATION:
                completeAddApplication(this, data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_SHORTCUT:
                processShortcut(data, REQUEST_PICK_APPLICATION, REQUEST_CREATE_SHORTCUT);
                break;
            case REQUEST_CREATE_SHORTCUT:
                completeAddShortcut(data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_LIVE_FOLDER:
                addLiveFolder(data);
                break;
            case REQUEST_CREATE_LIVE_FOLDER:
                completeAddLiveFolder(data, mAddItemCellInfo, !mDesktopLocked);
                break;
            case REQUEST_PICK_APPWIDGET:
                addAppWidget(data);
                break;
            case REQUEST_CREATE_APPWIDGET:
                completeAddAppWidget(data, mAddItemCellInfo, !mDesktopLocked);
                break;
            }
        } else if ((requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET)
                && resultCode == RESULT_CANCELED && data != null) {
            // Clean up the appWidgetId if we canceled
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        // Flag any binder to stop early before switching
        if (mBinder != null) {
            mBinder.mTerminate = true;
        }

        if (PROFILE_ROTATE) {
            android.os.Debug.startMethodTracing("/sdcard/launcher-rotate");
        }
        return null;
    }

    private boolean acceptFilter() {
        final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        return !inputManager.isFullscreenMode();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // this tells the framework to start tracking for
            // a long press and eventual key up. it will only
            // do so if this is the first down (not a repeat).
            event.startTracking();
            return true;
        }

        boolean handled = super.onKeyDown(keyCode, event);
        if (!handled && acceptFilter() && keyCode != KeyEvent.KEYCODE_ENTER) {
            boolean gotKey = TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
                    keyCode, event);
            if (gotKey && mDefaultKeySsb != null && mDefaultKeySsb.length() > 0) {
                // something usable has been typed - start a search
                // the typed text will be retrieved and cleared by
                // showSearchDialog()
                // If there are multiple keystrokes before the search dialog takes focus,
                // onSearchRequested() will be called for every keystroke,
                // but it is idempotent, so it's fine.
                return onSearchRequested();
            }
        }
        return handled;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
        	try {
        		if (!mScreenLayout.isShown() && !DeleteZone.sDragging)
        			new SwitcherTask().execute();
        	} catch (ClassCastException e) {
        		makeToast("Oh snap!");
        	}
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && event.isTracking() && !event.isCanceled() && (mAnimatedDrawer.getVisibility() != View.VISIBLE) && (!mScreenLayout.isShown())) {
            // if the call key is being released, AND we are tracking
            // it from an initial key down, AND it is not canceled,
            // then handle it.
            openOptionsMenu();
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private String getTypedText() {
        return mDefaultKeySsb.toString();
    }

    private void clearTypedText() {
        mDefaultKeySsb.clear();
        mDefaultKeySsb.clearSpans();
        Selection.setSelection(mDefaultKeySsb, 0);
    }

    /**
     * Restores the previous state, if it exists.
     * 
     * @param savedState
     *            The previous state.
     */
    private void restoreState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        final int currentScreen = savedState.getInt(RUNTIME_STATE_CURRENT_SCREEN, -1);
        if (currentScreen > -1) {
            mWorkspace.setCurrentScreen(currentScreen);
        }

        final int addScreen = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SCREEN, -1);
        if (addScreen > -1) {
            mAddItemCellInfo = new CellLayout.CellInfo();
            final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
            addItemCellInfo.valid = true;
            addItemCellInfo.screen = addScreen;
            addItemCellInfo.cellX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
            addItemCellInfo.cellY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
            addItemCellInfo.spanX = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
            addItemCellInfo.spanY = savedState.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
            addItemCellInfo.findVacantCellsFromOccupied(savedState
                    .getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS), savedState
                    .getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X), savedState
                    .getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y));
            mRestoring = true;
        }

        boolean renameFolder = savedState.getBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
        if (renameFolder) {
            long id = savedState.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
            mFolderInfo = sModel.getFolderById(this, id);
            mRestoring = true;
        }
    }

    private ScreenIndicator mIndicator;
    private ScreenLayout mScreenLayout;
    private DeleteZone mDeleteZone;

    /**
     * Finds all the views we need and configure them properly.
     */
    private void setupViews() {
    	
        mDragLayer = (DragLayer) findViewById(R.id.drag_layer);
        final DragLayer dragLayer = mDragLayer;

        mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
        final Workspace workspace = mWorkspace;

        mIndicator = (ScreenIndicator) dragLayer.findViewById(R.id.screen_indicator);
        mIndicator.setWorkspace(workspace);
        
        mAllAppsGrid = (AllAppsGridView) dragLayer.findViewById(R.id.app_apps_grid_view);
        final AllAppsGridView grid = mAllAppsGrid;

        mAnimatedDrawer = (AnimatedDrawer) dragLayer.findViewById(R.id.relativegridview);
    	
    	if (oldSchool)
    		mAnimatedDrawer.setAnimationStyle(R.anim.slideup, R.anim.slidedown);
    	else
    		mAnimatedDrawer.setAnimationStyle(R.anim.drawer_fade_in, R.anim.drawer_fade_out);

        mDeleteZone = (DeleteZone) dragLayer.findViewById(R.id.delete_zone);
        
        //Styles
        mFroyo = (RelativeLayout) dragLayer.findViewById(R.id.froyo_theme);
        mGingerbread = (RelativeLayout) dragLayer.findViewById(R.id.gingerbread_theme);
        mSense = (RelativeLayout) dragLayer.findViewById(R.id.sense_theme);
        mSlate = (RelativeLayout) dragLayer.findViewById(R.id.slate_theme);
        mMusic = (RelativeLayout) dragLayer.findViewById(R.id.music_theme);
        mBlackboard = (RelativeLayout) dragLayer.findViewById(R.id.blackboard_theme);
        mEspresso = (RelativeLayout) dragLayer.findViewById(R.id.espresso_theme);
        mBlur = (RelativeLayout) dragLayer.findViewById(R.id.blur_theme);
        mNinja = (RelativeLayout) dragLayer.findViewById(R.id.ninja_theme);
        mSamurai = (RelativeLayout) dragLayer.findViewById(R.id.samurai_theme);
        mPro = (RelativeLayout) dragLayer.findViewById(R.id.pro_theme);
        
        mDeleteZoneRelative = (AnimatedDrawer) dragLayer.findViewById(R.id.delete_zone_relative);
        ((AnimatedDrawer) mDeleteZoneRelative).setAnimationStyle(R.anim.drawer_fade_in, R.anim.drawer_fade_out);
        
        mScreenLayout = new ScreenLayout(((ViewStub) findViewById(R.id.stub_screen_switcher)).inflate());
        mScreenLayout.setScreenChangeListener(mScreenChangeListener);

        grid.setTextFilterEnabled(false);
        grid.setDragger(dragLayer);
        grid.setLauncher(this);

        workspace.setOnLongClickListener(this);
        workspace.setDragger(dragLayer);
        workspace.setLauncher(this);

        mDeleteZone.setLauncher(this);
        mDeleteZone.setDragController(dragLayer);
        mDeleteZone.setHandle(mDeleteZoneRelative);

        dragLayer.setIgnoredDropTarget(grid);
        dragLayer.setDragScoller(workspace);
        dragLayer.setDragListener(mDeleteZone);
    }
    
    private void styleSelect() {
    	SharedPreferences sp = Launcher.this.getSharedPreferences(STYLE_PREF, Context.MODE_PRIVATE);
    	Editor editor = sp.edit();
    	String pref = PREFERENCE_STYLE;
    	String pref_backup = PREFERENCE_STYLE + "_old";
    	int key = sp.getInt(pref , 0);
    	int key_old = sp.getInt(pref_backup, 0);
    	
    	switch (key_old) {
    	case 0:
        	mGingerbread.setVisibility(View.GONE);
        	break;
    	case 1:
    		mFroyo.setVisibility(View.GONE);
        	break;
    	case 2:
        	mSense.setVisibility(View.GONE);
        	break;
    	case 3:
        	mSlate.setVisibility(View.GONE);
        	break;
    	case 4:
        	mMusic.setVisibility(View.GONE);
        	break;
    	case 5:
        	mBlackboard.setVisibility(View.GONE);
        	break;
    	case 6:
        	mEspresso.setVisibility(View.GONE);
        	break;
    	case 7:
        	mBlur.setVisibility(View.GONE);
        	break;
    	case 8:
        	mNinja.setVisibility(View.GONE);
        	break;
    	case 9:
    		mSamurai.setVisibility(View.GONE);
        	break;
    	case 10:
    		mPro.setVisibility(View.GONE);
        	break;
    	}
    	
    	editor.putInt(pref_backup , key);
    	editor.commit();
    	
    	switch (key) {
    	case 0:
        	mGingerbread.setVisibility(View.VISIBLE);
        	break;
    	case 1:
    		mFroyo.setVisibility(View.VISIBLE);
        	break;
    	case 2:
        	mSense.setVisibility(View.VISIBLE);
        	break;
    	case 3:
        	mSlate.setVisibility(View.VISIBLE);
        	break;
    	case 4:
        	mMusic.setVisibility(View.VISIBLE);
        	break;
    	case 5:
        	mBlackboard.setVisibility(View.VISIBLE);
        	break;
    	case 6:
        	mEspresso.setVisibility(View.VISIBLE);
        	break;
    	case 7:
        	mBlur.setVisibility(View.VISIBLE);
        	break;
    	case 8:
        	mNinja.setVisibility(View.VISIBLE);
        	break;
    	case 9:
    		mSamurai.setVisibility(View.VISIBLE);
        	break;
    	case 10:
    		mPro.setVisibility(View.VISIBLE);
        	break;
    	}
    }
    
    @SuppressWarnings("unused")
	private void loadPreferences() {
    	Preferences.setLauncher(this);
    	SharedPreferences sharedPref = getSharedPreferences(STYLE_PREF, 0);
    	if (false)
    		Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, ((sharedPref.getBoolean(getResources().getString(R.string.preference_1_key), false)) ? 1 : 0));
    	hideLabels = sharedPref.getBoolean(getResources().getString(R.string.preference_3_key), false);
    	keepIndicatorOff = sharedPref.getBoolean(getResources().getString(R.string.preference_4_key), false);
    	enableScreenBounce = sharedPref.getBoolean(getResources().getString(R.string.preference_5_key), false);
    }

    /**
     * Creates a view representing a shortcut.
     * 
     * @param info
     *            The data structure describing the shortcut.
     * 
     * @return A View inflated from R.layout.application.
     */
    View createShortcut(ApplicationInfo info) {
        return createShortcut(R.layout.application, (ViewGroup) mWorkspace.getChildAt(mWorkspace
                .getCurrentScreen()), info);
    }

    /**
     * Creates a view representing a shortcut inflated from the specified resource.
     * 
     * @param layoutResId
     *            The id of the XML layout used to create the shortcut.
     * @param parent
     *            The group the shortcut belongs to.
     * @param info
     *            The data structure describing the shortcut.
     * 
     * @return A View inflated from layoutResId.
     */
    View createShortcut(int layoutResId, ViewGroup parent, ApplicationInfo info) {
        TextView favorite = (TextView) mInflater.inflate(layoutResId, parent, false);

        if (!info.filtered) {
            info.icon = Utilities.createIconThumbnail(info.icon, this);
            info.filtered = true;
        }

        favorite.setCompoundDrawablesWithIntrinsicBounds(null, info.icon, null, null);
        
        if (!hideLabels)
        	favorite.setText(info.title);
        
        favorite.setTag(info);
        favorite.setOnClickListener(this);

        return favorite;
    }

    /**
     * Add an application shortcut to the workspace.
     * 
     * @param data
     *            The intent describing the application.
     * @param cellInfo
     *            The position on screen where to create the shortcut.
     */
    void completeAddApplication(Context context, Intent data, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        cellInfo.screen = mWorkspace.getCurrentScreen();
        if (!findSingleSlot(cellInfo))
            return;

        final ApplicationInfo info = infoFromApplicationIntent(context, data);
        if (info != null) {
            mWorkspace.addApplicationShortcut(info, cellInfo, insertAtFirst);
        }
    }

    private static ApplicationInfo infoFromApplicationIntent(Context context, Intent data) {
        ComponentName component = data.getComponent();
        PackageManager packageManager = context.getPackageManager();
        ActivityInfo activityInfo = null;
        try {
            activityInfo = packageManager.getActivityInfo(component, 0 /* no flags */);
        } catch (NameNotFoundException e) {
            e(LOG_TAG, "Couldn't find ActivityInfo for selected application", e);
        }

        if (activityInfo != null) {
            ApplicationInfo itemInfo = new ApplicationInfo();

            itemInfo.title = activityInfo.loadLabel(packageManager);
            if (itemInfo.title == null) {
                itemInfo.title = activityInfo.name;
            }

            itemInfo.setActivity(component, Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            itemInfo.icon = activityInfo.loadIcon(packageManager);
            itemInfo.container = ItemInfo.NO_ID;

            return itemInfo;
        }

        return null;
    }

    /**
     * Add a shortcut to the workspace.
     * 
     * @param data
     *            The intent describing the shortcut.
     * @param cellInfo
     *            The position on screen where to create the shortcut.
     * @param insertAtFirst
     */
    private void completeAddShortcut(Intent data, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        cellInfo.screen = mWorkspace.getCurrentScreen();
        if (!findSingleSlot(cellInfo))
            return;

        final ApplicationInfo info = addShortcut(this, data, cellInfo, false);

        if (!mRestoring) {
            sModel.addDesktopItem(info);

            final View view = createShortcut(info);
            mWorkspace
                    .addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY, 1, 1, insertAtFirst);
        } else if (sModel.isDesktopLoaded()) {
            sModel.addDesktopItem(info);
        }
    }

    /**
     * Add a widget to the workspace.
     * 
     * @param data
     *            The intent describing the appWidgetId.
     * @param cellInfo
     *            The position on screen where to create the widget.
     */
    private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {

        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        if (LOGD)
            d(LOG_TAG, "dumping extras content=" + extras.toString());

        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        // Calculate the grid spans needed to fit this widget
        CellLayout layout = (CellLayout) mWorkspace.getChildAt(cellInfo.screen);
        int[] spans = layout.rectToCell(appWidgetInfo.minWidth, appWidgetInfo.minHeight);

        // Try finding open space on Launcher screen
        final int[] xy = mCellCoordinates;
        if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
            if (appWidgetId != -1)
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            return;
        }

        // Build Launcher-specific widget info and save to database
        LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(appWidgetId);
        launcherInfo.spanX = spans[0];
        launcherInfo.spanY = spans[1];

        LauncherModel.addItemToDatabase(this, launcherInfo,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, mWorkspace.getCurrentScreen(), xy[0],
                xy[1], false);

        if (!mRestoring) {
            sModel.addDesktopAppWidget(launcherInfo);

            // Perform actual inflation because we're live
            launcherInfo.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

            launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            launcherInfo.hostView.setTag(launcherInfo);

            mWorkspace.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1], launcherInfo.spanX,
                    launcherInfo.spanY, insertAtFirst);
        } else if (sModel.isDesktopLoaded()) {
            sModel.addDesktopAppWidget(launcherInfo);
        }
        // finish load a widget, send it an intent
        if(appWidgetInfo!=null)
        	appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
    }

    public LauncherAppWidgetHost getAppWidgetHost() {
        return mAppWidgetHost;
    }

    static ApplicationInfo addShortcut(Context context, Intent data, CellLayout.CellInfo cellInfo,
            boolean notify) {

        final ApplicationInfo info = infoFromShortcutIntent(context, data);
        LauncherModel.addItemToDatabase(context, info,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen, cellInfo.cellX,
                cellInfo.cellY, notify);

        return info;
    }

    private static ApplicationInfo infoFromShortcutIntent(Context context, Intent data) {
        Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        Bitmap bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

        Drawable icon = null;
        boolean filtered = false;
        boolean customIcon = false;
        ShortcutIconResource iconResource = null;

        if (bitmap != null) {
            icon = new FastBitmapDrawable(Utilities.createBitmapThumbnail(bitmap, context));
            filtered = true;
            customIcon = true;
        } else {
            Parcelable extra = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
            if (extra != null && extra instanceof ShortcutIconResource) {
                try {
                    iconResource = (ShortcutIconResource) extra;
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager
                            .getResourcesForApplication(iconResource.packageName);
                    final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                    icon = resources.getDrawable(id);
                } catch (Exception e) {
                    w(LOG_TAG, "Could not load shortcut icon: " + extra);
                }
            }
        }

        if (icon == null) {
            icon = context.getPackageManager().getDefaultActivityIcon();
        }

        final ApplicationInfo info = new ApplicationInfo();
        info.icon = icon;
        info.filtered = filtered;
        info.title = name;
        info.intent = intent;
        info.customIcon = customIcon;
        info.iconResource = iconResource;

        return info;
    }

    void closeSystemDialogs() {
        getWindow().closeAllPanels();

        try {
            dismissDialog(DIALOG_CREATE_SHORTCUT);
            // Unlock the workspace if the dialog was showing
            mWorkspace.unlock();
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        }

        try {
            dismissDialog(DIALOG_RENAME_FOLDER);
            // Unlock the workspace if the dialog was showing
            mWorkspace.unlock();
        } catch (Exception e) {
            // An exception is thrown if the dialog is not visible, which is fine
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Close the menu
        if (Intent.ACTION_MAIN.equals(intent.getAction())) {
            closeSystemDialogs();

            // Set this flag so that onResume knows to close the search dialog if it's open,
            // because this was a new intent (thus a press of 'home' or some such) rather than
            // for example onResume being called when the user pressed the 'back' button.
            mIsNewIntent = true;

            if ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) {

                if (!mWorkspace.isDefaultScreenShowing()) {
                    if (mIndicator != null && !keepIndicatorOn)
                        mIndicator.setVisibility(View.GONE);
                    mWorkspace.moveToDefaultScreen();
                }

                final View v = getWindow().peekDecorView();
                if (v != null && v.getWindowToken() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // NOTE: Do NOT do this. Ever. This is a terrible and horrifying hack.
        //
        // Home loads the content of the workspace on a background thread. This means that
        // a previously focused view will be, after orientation change, added to the view
        // hierarchy at an undeterminate time in the future. If we were to invoke
        // super.onRestoreInstanceState() here, the focus restoration would fail because the
        // view to focus does not exist yet.
        //
        // However, not invoking super.onRestoreInstanceState() is equally bad. In such a case,
        // panels would not be restored properly. For instance, if the menu is open then the
        // user changes the orientation, the menu would not be opened in the new orientation.
        //
        // To solve both issues Home messes up with the internal state of the bundle to remove
        // the properties it does not want to see restored at this moment. After invoking
        // super.onRestoreInstanceState(), it removes the panels state.
        //
        // Later, when the workspace is done loading, Home calls super.onRestoreInstanceState()
        // again to restore focus and other view properties. It will not, however, restore
        // the panels since at this point the panels' state has been removed from the bundle.
        //
        // This is a bad example, do not do this.
        //
        // If you are curious on how this code was put together, take a look at the following
        // in Android's source code:
        // - Activity.onRestoreInstanceState()
        // - PhoneWindow.restoreHierarchyState()
        // - PhoneWindow.DecorView.onAttachedToWindow()
        //
        // The source code of these various methods shows what states should be kept to
        // achieve what we want here.

        Bundle windowState = savedInstanceState.getBundle("android:viewHierarchyState");
        SparseArray<Parcelable> savedStates = null;
        int focusedViewId = View.NO_ID;

        if (windowState != null) {
            savedStates = windowState.getSparseParcelableArray("android:views");
            windowState.remove("android:views");
            focusedViewId = windowState.getInt("android:focusedViewId", View.NO_ID);
            windowState.remove("android:focusedViewId");
        }

        super.onRestoreInstanceState(savedInstanceState);

        if (windowState != null) {
            windowState.putSparseParcelableArray("android:views", savedStates);
            windowState.putInt("android:focusedViewId", focusedViewId);
            windowState.remove("android:Panels");
        }

        mSavedInstanceState = savedInstanceState;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(RUNTIME_STATE_CURRENT_SCREEN, mWorkspace.getCurrentScreen());

        final ArrayList<Folder> folders = mWorkspace.getOpenFolders();
        if (folders.size() > 0) {
            final int count = folders.size();
            long[] ids = new long[count];
            for (int i = 0; i < count; i++) {
                final FolderInfo info = folders.get(i).getInfo();
                ids[i] = info.id;
            }
            outState.putLongArray(RUNTIME_STATE_USER_FOLDERS, ids);
        }

        if (mAddItemCellInfo != null && mAddItemCellInfo.valid && mWaitingForResult) {
            final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
            final CellLayout layout = (CellLayout) mWorkspace.getChildAt(addItemCellInfo.screen);

            outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN, addItemCellInfo.screen);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X, addItemCellInfo.cellX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y, addItemCellInfo.cellY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X, addItemCellInfo.spanX);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y, addItemCellInfo.spanY);
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X, layout.getCountX());
            outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y, layout.getCountY());
            outState.putBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS, layout
                    .getOccupiedCells());
        }

        if (mFolderInfo != null && mWaitingForResult) {
            outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
            outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID, mFolderInfo.id);
        }
    }

    @Override
    public void onDestroy() {
        mDestroyed = true;

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);

        super.onDestroy();

        try {
            mAppWidgetHost.stopListening();
        } catch (NullPointerException ex) {
            w(LOG_TAG, "problem while stopping AppWidgetHost during Launcher destruction", ex);
        }

        TextKeyListener.getInstance().release();

        mAllAppsGrid.clearTextFilter();
        mAllAppsGrid.setAdapter(null);
        sModel.unbind();
        sModel.abortLoaders();
        mWorkspace.unbindWidgetScrollableViews();

        getContentResolver().unregisterContentObserver(mObserver);
        getContentResolver().unregisterContentObserver(mWidgetObserver);
        unregisterReceiver(mApplicationsReceiver);
        unregisterReceiver(mCloseSystemDialogsReceiver);

        mWorkspace.unregisterProvider();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode >= 0)
            mWaitingForResult = true;
        try {
        	super.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
        	makeToast(GENERIC);
        }
    }

    @Override
    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData,
            boolean globalSearch) {

        // Slide the search widget to the top, if it's on the current screen,
        // otherwise show the search dialog immediately.
        Search searchWidget = mWorkspace.findSearchWidgetOnCurrentScreen();
        if (searchWidget == null) {
            showSearchDialog(initialQuery, selectInitialQuery, appSearchData, globalSearch);
        } else {
            searchWidget.startSearch(initialQuery, selectInitialQuery, appSearchData, globalSearch);
            // show the currently typed text in the search widget while sliding
            searchWidget.setQuery(getTypedText());
        }
    }

    /**
     * Show the search dialog immediately, without changing the search widget.
     * 
     * @see Activity#startSearch(String, boolean, android.os.Bundle, boolean)
     */
    void showSearchDialog(String initialQuery, boolean selectInitialQuery, Bundle appSearchData,
            boolean globalSearch) {

        if (initialQuery == null) {
            // Use any text typed in the launcher as the initial query
            initialQuery = getTypedText();
            clearTypedText();
        }
        if (appSearchData == null) {
            appSearchData = new Bundle();
            appSearchData.putString("source", "launcher-search");
        }

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        final Search searchWidget = mWorkspace.findSearchWidgetOnCurrentScreen();
        if (searchWidget != null) {
            // This gets called when the user leaves the search dialog to go back to
            // the Launcher.
            searchManager.setOnCancelListener(new SearchManager.OnCancelListener() {
                public void onCancel() {
                    searchManager.setOnCancelListener(null);
                    stopSearch();
                }
            });
        }

        searchManager.startSearch(initialQuery, selectInitialQuery, getComponentName(),
                appSearchData, globalSearch);
    }

    /**
     * Cancel search dialog if it is open.
     */
    void stopSearch() {
        // Close search dialog
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchManager.stopSearch();
        // Restore search widget to its normal position
        Search searchWidget = mWorkspace.findSearchWidgetOnCurrentScreen();
        if (searchWidget != null) {
            searchWidget.stopSearch(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDesktopLocked && mSavedInstanceState == null)
            return false;

        super.onCreateOptionsMenu(menu);
        menu.add(MENU_GROUP_ADD, MENU_ADD, 0, R.string.menu_add).setIcon(
                android.R.drawable.ic_menu_add).setAlphabeticShortcut('A');
        menu.add(0, MENU_WALLPAPER_SETTINGS, 0, R.string.menu_wallpaper).setIcon(
                android.R.drawable.ic_menu_gallery).setAlphabeticShortcut('W');
        menu.add(0, MENU_SEARCH, 0, R.string.menu_search).setIcon(
                android.R.drawable.ic_search_category_default).setAlphabeticShortcut(
                SearchManager.MENU_KEY);
        menu.add(0, MENU_PREFERENCES, 0, R.string.menu_preferences).setIcon(
                R.drawable.ic_menu_customize);

        final Intent settings = new Intent(android.provider.Settings.ACTION_SETTINGS);
        settings
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings).setIcon(
                android.R.drawable.ic_menu_preferences).setAlphabeticShortcut('P').setIntent(
                settings);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        
        if(mIsEditMode || mIsWidgetEditMode)return false;
        
        // We can't trust the view state here since views we may not be done binding.
        // Get the vacancy state from the model instead.
        mMenuAddInfo = mWorkspace.findAllVacantCellsFromModel();
        menu.setGroupEnabled(MENU_GROUP_ADD, mMenuAddInfo != null && mMenuAddInfo.valid);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ADD:
            addItems();
            return true;
        case MENU_WALLPAPER_SETTINGS:
            startWallpaper();
            return true;
        case MENU_SEARCH:
            onSearchRequested();
            return true;
        case MENU_PREFERENCES:
        	Intent preferencesActivity = new Intent(getBaseContext(),
                    Preferences.class);
        	startActivity(preferencesActivity);
        	overridePendingTransition(R.anim.drawer_fade_in, R.anim.drawer_fade_out);
        	return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Indicates that we want global search for this activity by setting the globalSearch argument
     * for {@link #startSearch} to true.
     */

    @Override
    public boolean onSearchRequested() {
        startSearch(null, false, null, true);
        return true;
    }

    private void addItems() {
        showAddDialog(mMenuAddInfo);
    }

    private void removeShortcutsForPackage(String packageName) {
        if (packageName != null && packageName.length() > 0) {
            mWorkspace.removeShortcutsForPackage(packageName);
        }
    }

    private void updateShortcutsForPackage(String packageName) {
        if (packageName != null && packageName.length() > 0) {
            mWorkspace.updateShortcutsForPackage(packageName);
        }
    }

    void addAppWidget(Intent data) {
        // TODO: catch bad widget exception when sent
        int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

        String customWidget = data.getStringExtra(EXTRA_CUSTOM_WIDGET);
        if (SEARCH_WIDGET.equals(customWidget)) {
            // We don't need this any more, since this isn't a real app widget.
            mAppWidgetHost.deleteAppWidgetId(appWidgetId);
            // add the search widget
            addSearch();
        } else {
            AppWidgetProviderInfo appWidget = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

            try 
            { 
                Bundle metadata = getPackageManager().getReceiverInfo(appWidget.provider, 
                                                             PackageManager.GET_META_DATA).metaData; 
                if (metadata != null) {
	                if (metadata.containsKey(LauncherMetadata.Requirements.APIVersion)) 
	                { 
	                        int requiredApiVersion = metadata.getInt(LauncherMetadata.Requirements.APIVersion); 
	                        if (requiredApiVersion > LauncherMetadata.CurrentAPIVersion) 
	                        { 
	                        	onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_CANCELED, data);
	                        	// Show a nice toast here to tell the user why the widget is rejected.
	                        	return;
	                        }
	                        // If there are Settings for scrollable or animations test them here too!
	                } 
                }
            } 
            catch(PackageManager.NameNotFoundException expt) 
            { 
                // No Metadata available... then it is all OK... 
            } 
            
            
            if (appWidget.configure != null) {
                // Launch over to configure widget, if needed
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidget.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

                startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
            } else {
                // Otherwise just add it
                onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
            }
        }
    }

    void addSearch() {
        final Widget info = Widget.makeSearch();
        final CellLayout.CellInfo cellInfo = mAddItemCellInfo;

        final int[] xy = mCellCoordinates;
        final int spanX = info.spanX;
        final int spanY = info.spanY;

        if (!findSlot(cellInfo, xy, spanX, spanY))
            return;

        sModel.addDesktopItem(info);
        LauncherModel.addItemToDatabase(this, info, LauncherSettings.Favorites.CONTAINER_DESKTOP,
                mWorkspace.getCurrentScreen(), xy[0], xy[1], false);

        final View view = mInflater.inflate(info.layoutResource, null);
        view.setTag(info);
        Search search = (Search) view.findViewById(R.id.widget_search);
        search.setLauncher(this);

        mWorkspace.addInCurrentScreen(view, xy[0], xy[1], info.spanX, spanY);
    }

    void processShortcut(Intent intent, int requestCodeApplication, int requestCodeShortcut) {
        // Handle case where user selected "Applications"
        String applicationName = getResources().getString(R.string.group_applications);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (applicationName != null && applicationName.equals(shortcutName)) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
            pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
            startActivityForResult(pickIntent, requestCodeApplication);
        } else {
            startActivityForResult(intent, requestCodeShortcut);
        }
    }

    void addLiveFolder(Intent intent) {
        // Handle case where user selected "Folder"
        String folderName = getResources().getString(R.string.group_folder);
        String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

        if (folderName != null && folderName.equals(shortcutName)) {
            addFolder(!mDesktopLocked);
        } else {
            startActivityForResult(intent, REQUEST_CREATE_LIVE_FOLDER);
        }
    }

    void addFolder(boolean insertAtFirst) {
        UserFolderInfo folderInfo = new UserFolderInfo();
        folderInfo.title = getText(R.string.folder_name);

        CellLayout.CellInfo cellInfo = mAddItemCellInfo;
        cellInfo.screen = mWorkspace.getCurrentScreen();
        if (!findSingleSlot(cellInfo))
            return;

        // Update the model
        LauncherModel.addItemToDatabase(this, folderInfo,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, mWorkspace.getCurrentScreen(),
                cellInfo.cellX, cellInfo.cellY, false);
        sModel.addDesktopItem(folderInfo);
        sModel.addFolder(folderInfo);

        // Create the view
        FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this,
                (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentScreen()), folderInfo);
        mWorkspace.addInCurrentScreen(newFolder, cellInfo.cellX, cellInfo.cellY, 1, 1,
                insertAtFirst);
    }

    private void completeAddLiveFolder(Intent data, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        cellInfo.screen = mWorkspace.getCurrentScreen();
        if (!findSingleSlot(cellInfo))
            return;

        final LiveFolderInfo info = addLiveFolder(this, data, cellInfo, false);

        if (!mRestoring) {
            sModel.addDesktopItem(info);

            final View view = LiveFolderIcon.fromXml(R.layout.live_folder_icon, this, (ViewGroup) mWorkspace.getChildAt(mWorkspace.getCurrentScreen()), info);
            mWorkspace
                    .addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY, 1, 1, insertAtFirst);
        } else if (sModel.isDesktopLoaded()) {
            sModel.addDesktopItem(info);
        }
    }

    static LiveFolderInfo addLiveFolder(Context context, Intent data, CellLayout.CellInfo cellInfo,
            boolean notify) {

        Intent baseIntent = data.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT);
        String name = data.getStringExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME);

        Drawable icon = null;
        boolean filtered = false;
        Intent.ShortcutIconResource iconResource = null;

        Parcelable extra = data.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON);
        if (extra != null && extra instanceof Intent.ShortcutIconResource) {
            try {
                iconResource = (Intent.ShortcutIconResource) extra;
                final PackageManager packageManager = context.getPackageManager();
                Resources resources = packageManager
                        .getResourcesForApplication(iconResource.packageName);
                final int id = resources.getIdentifier(iconResource.resourceName, null, null);
                icon = resources.getDrawable(id);
            } catch (Exception e) {
                w(LOG_TAG, "Could not load live folder icon: " + extra);
            }
        }

        if (icon == null) {
            icon = context.getResources().getDrawable(R.drawable.ic_launcher_folder);
        }

        final LiveFolderInfo info = new LiveFolderInfo();
        info.icon = icon;
        info.filtered = filtered;
        info.title = name;
        info.iconResource = iconResource;
        info.uri = data.getData();
        info.baseIntent = baseIntent;
        info.displayMode = data.getIntExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
                LiveFolders.DISPLAY_MODE_GRID);

        LauncherModel.addItemToDatabase(context, info,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen, cellInfo.cellX,
                cellInfo.cellY, notify);
        sModel.addFolder(info);

        return info;
    }

    private boolean findSingleSlot(CellLayout.CellInfo cellInfo) {
        final int[] xy = new int[2];
        if (findSlot(cellInfo, xy, 1, 1)) {
            cellInfo.cellX = xy[0];
            cellInfo.cellY = xy[1];
            return true;
        }
        return false;
    }

    private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX, int spanY) {
        if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
            boolean[] occupied = mSavedState != null ? mSavedState
                    .getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS) : null;
            cellInfo = mWorkspace.findAllVacantCells(occupied);
            if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
                makeToast(OUT_OF_SPACE);
                return false;
            }
        }
        return true;
    }

    private void startWallpaper() {
        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
        Intent chooser = Intent.createChooser(pickWallpaper, getText(R.string.chooser_wallpaper));
        WallpaperManager wm = (WallpaperManager) getSystemService(Context.WALLPAPER_SERVICE);

        // TODO WallpaperInfo wi = wm.getWallpaperInfo();

        try {
            Method getWallpaperInfo = wm.getClass().getMethod("getWallpaperInfo");
            Object wi = getWallpaperInfo.invoke(wm);

            Method getPackageName = wi.getClass().getMethod("getPackageName");
            String pname = (String) getPackageName.invoke(wi);

            Method getSettingsActivity = wi.getClass().getMethod("getSettingsActivity");
            String activity = (String) getSettingsActivity.invoke(wi);

            if (wi != null && activity != null) {
                LabeledIntent li = new LabeledIntent(getPackageName(),
                        R.string.configure_wallpaper, 0);
                li.setClassName(pname, activity);
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
            }
        } catch (Exception e) {

        }

        startActivity(chooser);
    }

    /**
     * Registers various intent receivers. The current implementation registers only a wallpaper
     * intent receiver to let other applications change the wallpaper.
     */
    private void registerIntentReceivers() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mApplicationsReceiver, filter);
        filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        registerReceiver(mCloseSystemDialogsReceiver, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        registerReceiver(mApplicationsReceiver, filter);
    }

    /**
     * Registers various content observers. The current implementation registers only a favorites
     * observer to keep track of the favorites applications.
     */
    private void registerContentObservers() {
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true, mObserver);
        resolver.registerContentObserver(LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true,
                mWidgetObserver);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
            	if (mAnimatedDrawer.getVisibility() == View.VISIBLE)
                	AnimatedDrawerClose(true);
                return true;
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_BACK:
                if (!event.isCanceled()) {
                    mWorkspace.dispatchKeyEvent(event);
                    closeFolder();
                    if (mScreenLayout != null && mScreenLayout.isShown())
                        mScreenLayout.fadeOut();
                    if(mIsWidgetEditMode){
                        stopWidgetEdit();
                    }
                }
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private boolean closeFolder() {
        Folder folder = mWorkspace.getOpenFolder();
        if (folder != null) {
            closeFolder(folder);
            return true;
        }
        return false;
    }

    void closeFolder(Folder folder) {
        folder.getInfo().opened = false;
        ViewGroup parent = (ViewGroup) folder.getParent();
        if (parent != null) {
            parent.removeView(folder);
        }
        folder.onClose();
    }

    /**
     * When the notification that favorites have changed is received, requests a favorites list
     * refresh.
     */
    private void onFavoritesChanged() {
        mDesktopLocked = true;
        sModel.loadUserItems(false, this, false, false);
    }

    /**
     * Re-listen when widgets are reset.
     */
    private void onAppWidgetReset() {
        mAppWidgetHost.startListening();
    }

    void onDesktopItemsLoaded(ArrayList<ItemInfo> shortcuts,
            ArrayList<LauncherAppWidgetInfo> appWidgets) {
        if (mDestroyed) {
            if (LauncherModel.DEBUG_LOADERS) {
                d(LauncherModel.LOG_TAG, "  ------> destroyed, ignoring desktop items");
            }
            return;
        }
        bindDesktopItems(shortcuts, appWidgets);
    }

    /**
     * Refreshes the shortcuts shown on the workspace.
     */
    private void bindDesktopItems(ArrayList<ItemInfo> shortcuts,
            ArrayList<LauncherAppWidgetInfo> appWidgets) {

        final ApplicationsAdapter drawerAdapter = sModel.getApplicationsAdapter();
        if (shortcuts == null || appWidgets == null || drawerAdapter == null) {
            if (LauncherModel.DEBUG_LOADERS)
                d(LauncherModel.LOG_TAG, "  ------> a source is null");
            return;
        }

        final Workspace workspace = mWorkspace;
        int count = workspace.getChildCount();
        for (int i = 0; i < count; i++) {
            ((ViewGroup) workspace.getChildAt(i)).removeAllViewsInLayout();
        }

        if (DEBUG_USER_INTERFACE) {
            android.widget.Button finishButton = new android.widget.Button(this);
            finishButton.setText("Finish");
            workspace.addInScreen(finishButton, 1, 0, 0, 1, 1);

            finishButton.setOnClickListener(new android.widget.Button.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Flag any old binder to terminate early
        if (mBinder != null) {
            mBinder.mTerminate = true;
        }

        mBinder = new DesktopBinder(this, shortcuts, appWidgets, drawerAdapter);
        mBinder.startBindingItems();
    }

    private void bindItems(Launcher.DesktopBinder binder, ArrayList<ItemInfo> shortcuts, int start,
            int count) {

        final Workspace workspace = mWorkspace;
        final boolean desktopLocked = mDesktopLocked;

        final int end = Math.min(start + DesktopBinder.ITEMS_COUNT, count);
        int i = start;

        final int screenNumber = workspace.getChildCount();

        for (; i < end; i++) {
            final ItemInfo item = shortcuts.get(i);
            if (item.screen >= screenNumber)
                continue;
            switch (item.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                final View shortcut = createShortcut((ApplicationInfo) item);
                workspace.addInScreen(shortcut, item.screen, item.cellX, item.cellY, 1, 1,
                        !desktopLocked);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                final FolderIcon newFolder = FolderIcon.fromXml(R.layout.folder_icon, this,
                        (ViewGroup) workspace.getChildAt(workspace.getCurrentScreen()),
                        (UserFolderInfo) item);
                workspace.addInScreen(newFolder, item.screen, item.cellX, item.cellY, 1, 1,
                        !desktopLocked);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
                final FolderIcon newLiveFolder = LiveFolderIcon.fromXml(R.layout.live_folder_icon,
                        this, (ViewGroup) workspace.getChildAt(workspace.getCurrentScreen()),
                        (LiveFolderInfo) item);
                workspace.addInScreen(newLiveFolder, item.screen, item.cellX, item.cellY, 1, 1,
                        !desktopLocked);
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
                final int screen = workspace.getCurrentScreen();
                final View view = mInflater.inflate(R.layout.widget_search, (ViewGroup) workspace
                        .getChildAt(screen), false);

                Search search = (Search) view.findViewById(R.id.widget_search);
                search.setLauncher(this);

                final Widget widget = (Widget) item;
                view.setTag(widget);

                workspace.addWidget(view, widget, !desktopLocked);
                break;
            }
        }

        workspace.requestLayout();

        if (end >= count) {
        	try {
        		finishBindDesktopItems();
        	} catch (IllegalArgumentException e) {
        		makeToast(GENERIC);
        	}
            binder.startBindingDrawer();
        } else {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_ITEMS, i, count).sendToTarget();
        }
    }

    private void finishBindDesktopItems() {
        if (mSavedState != null) {
            if (!mWorkspace.hasFocus()) {
                mWorkspace.getChildAt(mWorkspace.getCurrentScreen()).requestFocus();
            }

            final long[] userFolders = mSavedState.getLongArray(RUNTIME_STATE_USER_FOLDERS);
            if (userFolders != null) {
                for (long folderId : userFolders) {
                    final FolderInfo info = sModel.findFolderById(folderId);
                    if (info != null) {
                        openFolder(info);
                    }
                }
                final Folder openFolder = mWorkspace.getOpenFolder();
                if (openFolder != null) {
                    openFolder.requestFocus();
                }
            }
            mSavedState = null;
        }

        if (mSavedInstanceState != null) {
            super.onRestoreInstanceState(mSavedInstanceState);
            mSavedInstanceState = null;
        }

        mDesktopLocked = false;
    }

    private void bindDrawer(Launcher.DesktopBinder binder, ApplicationsAdapter drawerAdapter) {
    	mAllAppsGrid.setAdapter(drawerAdapter);
        binder.startBindingAppWidgetsWhenIdle();
    }

    private void bindAppWidgets(Launcher.DesktopBinder binder,
            LinkedList<LauncherAppWidgetInfo> appWidgets) {

        final Workspace workspace = mWorkspace;
        final boolean desktopLocked = mDesktopLocked;

        if (!appWidgets.isEmpty()) {
            final LauncherAppWidgetInfo item = appWidgets.removeFirst();

            final int appWidgetId = item.appWidgetId;
            final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
                    .getAppWidgetInfo(appWidgetId);
            item.hostView = mAppWidgetHost.createView(this, appWidgetId, appWidgetInfo);

            if (LOGD) {
                d(LOG_TAG, String.format("about to setAppWidget for id=%d, info=%s", appWidgetId,
                        appWidgetInfo));
            }

            item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
            item.hostView.setTag(item);

            workspace.addInScreen(item.hostView, item.screen, item.cellX, item.cellY, item.spanX,
                    item.spanY, !desktopLocked);

            workspace.requestLayout();

            // finish load a widget, send it an intent
            if (appWidgetInfo != null)
                appwidgetReadyBroadcast(appWidgetId, appWidgetInfo.provider);
        }

        if (appWidgets.isEmpty()) {
            if (PROFILE_ROTATE) {
                android.os.Debug.stopMethodTracing();
            }
        } else {
            binder.obtainMessage(DesktopBinder.MESSAGE_BIND_APPWIDGETS).sendToTarget();
        }
    }

    private void appwidgetReadyBroadcast(int appWidgetId, ComponentName cname) {
        Intent ready = new Intent(LauncherIntent.Action.ACTION_READY).putExtra(
                LauncherIntent.Extra.EXTRA_APPWIDGET_ID, appWidgetId).putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId).putExtra(
                LauncherIntent.Extra.EXTRA_API_VERSION, LauncherMetadata.CurrentAPIVersion).		
                setComponent(cname);
        sendBroadcast(ready);
    }

    class SwitcherTask extends AsyncTask<Integer, Integer, Integer> {

        final int count = mWorkspace.getChildCount();

        @Override
        protected Integer doInBackground(Integer... params) {
            CellLayout screenView;
            // Create screen array
            if (sScreens == null || sScreens.length != count)
                sScreens = new Bitmap[count];

            for (int i = 0; i < count; i++) {
                // Prepare cache
                screenView = (CellLayout) mWorkspace.getChildAt(i);
                screenView.saveThumb();
                sScreens[i] = screenView.mThumb;
            }

            return null;
        }

        protected void onPreExecute() {}

        protected void onPostExecute(Integer result) {
            mScreenLayout.show(Launcher.this, sScreens);
        }

    }

    static Bitmap[] sScreens;

    final onScreenChangeListener mScreenChangeListener = new onScreenChangeListener() {

        @Override
        public void onScreenChange(int toScreen) {
            mWorkspace.snapToScreen(toScreen);
        }

        @Override
        public void onScreenManagerClosed() {
            if (sScreens == null)
                return;
        }

        @Override
        public void onScreenManagerOpened() {

        }

    };

    /**
     * Launches the intent referred by the clicked shortcut.
     * 
     * @param v
     *            The view representing the clicked shortcut.
     */
    public void onClick(View v) {
        Object tag = v.getTag();
        if (tag instanceof ApplicationInfo) {
            // Open shortcut
            final Intent intent = ((ApplicationInfo) tag).intent;
            // set bound
            if (v != null) {
                Rect targetRect = new Rect();
                v.getGlobalVisibleRect(targetRect);
                intent.setSourceBounds(targetRect);
            }
            startActivitySafely(intent);
        } else if (tag instanceof FolderInfo) {
            handleFolderClick((FolderInfo) tag);
        }
    }

    public void startActivitySafely(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            makeToast(ACTIVITY_NOT_FOUND);
        } catch (SecurityException e) {
            makeToast(ACTIVITY_NOT_FOUND);
            e(LOG_TAG, "Launcher does not have the permission to launch " + intent
                    + ". Make sure to create a MAIN intent-filter for the corresponding activity "
                    + "or use the exported attribute for this activity.", e);
        }
    }

    private void handleFolderClick(FolderInfo folderInfo) {
        if (!folderInfo.opened) {
            // Close any open folder
            closeFolder();
            // Open the requested folder
            openFolder(folderInfo);
        } else {
            // Find the open folder...
            Folder openFolder = mWorkspace.getFolderForTag(folderInfo);
            int folderScreen;
            if (openFolder != null) {
                folderScreen = mWorkspace.getScreenForView(openFolder);
                // .. and close it
                closeFolder(openFolder);
                if (folderScreen != mWorkspace.getCurrentScreen()) {
                    // Close any folder open on the current screen
                    closeFolder();
                    // Pull the folder onto this screen
                    openFolder(folderInfo);
                }
            }
        }
    }

    /**
     * Opens the user fodler described by the specified tag. The opening of the folder is animated
     * relative to the specified View. If the View is null, no animation is played.
     * 
     * @param folderInfo
     *            The FolderInfo describing the folder to open.
     */
    private void openFolder(FolderInfo folderInfo) {
        Folder openFolder;

        if (folderInfo instanceof UserFolderInfo) {
            openFolder = UserFolder.fromXml(this);
        } else if (folderInfo instanceof LiveFolderInfo) {
            openFolder = LiveFolder.fromXml(this, folderInfo);
        } else {
            return;
        }

        openFolder.setDragger(mDragLayer);
        openFolder.setLauncher(this);

        openFolder.bind(folderInfo);
        folderInfo.opened = true;

        mWorkspace.addInScreen(openFolder, folderInfo.screen, 0, 0, 4, 4);
        openFolder.onOpen();
    }

    /**
     * Returns true if the workspace is being loaded. When the workspace is loading, no user
     * interaction should be allowed to avoid any conflict.
     * 
     * @return True if the workspace is locked, false otherwise.
     */
    boolean isWorkspaceLocked() {
        return mDesktopLocked;
    }

    @Override
    public boolean onLongClick(View v) {
        if (mDesktopLocked) {
            return false;
        }

        if (!(v instanceof CellLayout)) {
            v = (View) v.getParent();
        }

        CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) v.getTag();

        // This happens when long clicking an item with the dpad/trackball
        if (cellInfo == null) {
            return true;
        }

        if (mWorkspace.allowLongPress()) {
            if (cellInfo.cell == null) {
                if (cellInfo.valid) {
                    // User long pressed on empty space
                    mWorkspace.setAllowLongPress(false);
                    showAddDialog(cellInfo);
                }
            } else {
                if (!(cellInfo.cell instanceof Folder)) {
                    // User long pressed on an item
                    mWorkspace.startDrag(cellInfo);
                }
            }
        }
        return true;
    }

    static LauncherModel getModel() {
        return sModel;
    }

    public Workspace getWorkspace() {
        return mWorkspace;
    }

    GridView getApplicationsGrid() {
        return mAllAppsGrid;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_CREATE_SHORTCUT:
            return new CreateShortcut().createDialog();
        case DIALOG_RENAME_FOLDER:
            return new RenameFolder().createDialog();
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_CREATE_SHORTCUT:
            break;
        case DIALOG_RENAME_FOLDER:
            if (mFolderInfo != null) {
                EditText input = (EditText) dialog.findViewById(R.id.folder_name);
                final CharSequence text = mFolderInfo.title;
                input.setText(text);
                input.setSelection(0, text.length());
            }
            break;
        }
    }

    void showRenameDialog(FolderInfo info) {
        mFolderInfo = info;
        mWaitingForResult = true;
        showDialog(DIALOG_RENAME_FOLDER);
    }

    private void showAddDialog(CellLayout.CellInfo cellInfo) {
        mAddItemCellInfo = cellInfo;
        mWaitingForResult = true;
        showDialog(DIALOG_CREATE_SHORTCUT);
    }

    private void pickShortcut(int requestCode, int title) {
        Bundle bundle = new Bundle();

        ArrayList<String> shortcutNames = new ArrayList<String>();
        shortcutNames.add(getString(R.string.group_applications));
        bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

        ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
        shortcutIcons.add(ShortcutIconResource.fromContext(Launcher.this,
                R.drawable.ic_launcher_application));
        bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

        Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
        pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(Intent.ACTION_CREATE_SHORTCUT));
        pickIntent.putExtra(Intent.EXTRA_TITLE, getText(title));
        pickIntent.putExtras(bundle);

        startActivityForResult(pickIntent, requestCode);
    }

    private class RenameFolder {
        private EditText mInput;

        Dialog createDialog() {
            mWaitingForResult = true;
            final View layout = View.inflate(Launcher.this, R.layout.rename_folder, null);
            mInput = (EditText) layout.findViewById(R.id.folder_name);

            AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setIcon(0);
            builder.setTitle(getString(R.string.rename_folder_title));
            builder.setCancelable(true);
            builder.setOnCancelListener(new Dialog.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    cleanup();
                }
            });
            builder.setNegativeButton(getString(R.string.cancel_action),
                    new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            cleanup();
                        }
                    });
            builder.setPositiveButton(getString(R.string.rename_action),
                    new Dialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            changeFolderName();
                        }
                    });
            builder.setView(layout);

            final AlertDialog dialog = builder.create();

            return dialog;
        }

        private void changeFolderName() {
            final String name = mInput.getText().toString();
            if (!TextUtils.isEmpty(name)) {
                // Make sure we have the right folder info
                mFolderInfo = sModel.findFolderById(mFolderInfo.id);
                mFolderInfo.title = name;
                LauncherModel.updateItemInDatabase(Launcher.this, mFolderInfo);

                if (mDesktopLocked) {
                    sModel.loadUserItems(false, Launcher.this, false, false);
                } else {
                    final FolderIcon folderIcon = (FolderIcon) mWorkspace
                            .getViewForTag(mFolderInfo);
                    if (folderIcon != null) {
                        folderIcon.setText(name);
                        getWorkspace().requestLayout();
                    } else {
                        mDesktopLocked = true;
                        sModel.loadUserItems(false, Launcher.this, false, false);
                    }
                }
            }
            cleanup();
        }

        private void cleanup() {
            mWorkspace.unlock();
            dismissDialog(DIALOG_RENAME_FOLDER);
            mWaitingForResult = false;
            mFolderInfo = null;
        }
    }

    /**
     * Displays the shortcut creation dialog and launches, if necessary, the appropriate activity.
     */
    private class CreateShortcut implements DialogInterface.OnClickListener,
            DialogInterface.OnCancelListener, DialogInterface.OnDismissListener {

        private AddAdapter mAdapter;

        Dialog createDialog() {
            mWaitingForResult = true;

            mAdapter = new AddAdapter(Launcher.this);

            final AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
            builder.setTitle(getString(R.string.menu_item_add_item));
            builder.setAdapter(mAdapter, this);

            builder.setInverseBackgroundForced(true);

            AlertDialog dialog = builder.create();
            dialog.setOnCancelListener(this);
            dialog.setOnDismissListener(this);

            return dialog;
        }

        public void onCancel(DialogInterface dialog) {
            mWaitingForResult = false;
            cleanup();
        }

        public void onDismiss(DialogInterface dialog) {
            mWorkspace.unlock();
        }

        private void cleanup() {
            mWorkspace.unlock();
            dismissDialog(DIALOG_CREATE_SHORTCUT);
        }

        /**
         * Handle the action clicked in the "Add to home" dialog.
         */
        public void onClick(DialogInterface dialog, int which) {
            Resources res = getResources();
            cleanup();

            switch (which) {
            case AddAdapter.ITEM_SHORTCUT: {
                // Insert extra item to handle picking application
                pickShortcut(REQUEST_PICK_SHORTCUT, R.string.title_select_shortcut);
                break;
            }

            case AddAdapter.ITEM_APPWIDGET: {
                int appWidgetId = Launcher.this.mAppWidgetHost.allocateAppWidgetId();

                Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
                pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                // add the search widget
                ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<AppWidgetProviderInfo>();
                AppWidgetProviderInfo info = new AppWidgetProviderInfo();
                info.provider = new ComponentName(getPackageName(), "XXX.YYY");
                info.label = getString(R.string.group_search);
                info.icon = R.drawable.ic_search_widget;
                customInfo.add(info);
                pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO,
                        customInfo);
                ArrayList<Bundle> customExtras = new ArrayList<Bundle>();
                Bundle b = new Bundle();
                b.putString(EXTRA_CUSTOM_WIDGET, SEARCH_WIDGET);
                customExtras.add(b);
                pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
                        customExtras);
                // start the pick activity
                startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
                break;
            }

            case AddAdapter.ITEM_LIVE_FOLDER: {
                // Insert extra item to handle inserting folder
                Bundle bundle = new Bundle();

                ArrayList<String> shortcutNames = new ArrayList<String>();
                shortcutNames.add(res.getString(R.string.group_folder));
                bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

                ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
                shortcutIcons.add(ShortcutIconResource.fromContext(Launcher.this,
                        R.drawable.ic_launcher_folder));
                bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

                Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(
                        LiveFolders.ACTION_CREATE_LIVE_FOLDER));
                pickIntent.putExtra(Intent.EXTRA_TITLE, getText(R.string.title_select_live_folder));
                pickIntent.putExtras(bundle);

                startActivityForResult(pickIntent, REQUEST_PICK_LIVE_FOLDER);
                break;
            }

            case AddAdapter.ITEM_WALLPAPER: {
                startWallpaper();
                break;
            }
            
            }
        }

    }

    /**
     * Receives notifications when applications are added/removed.
     */
    private class ApplicationsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final boolean replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
            String packageName = "";
            
            try {
            	packageName = intent.getData().getSchemeSpecificPart();
            } catch (NullPointerException e) {
            	// Ignore
            }

            if (LauncherModel.DEBUG_LOADERS) {
                d(LauncherModel.LOG_TAG, "application intent received: " + action + ", replacing="
                        + replacing);
                d(LauncherModel.LOG_TAG, "  --> " + intent.getData());
            }

            if (!Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
                if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                    if (!replacing) {
                        removeShortcutsForPackage(packageName);
                        if (LauncherModel.DEBUG_LOADERS) {
                            d(LauncherModel.LOG_TAG, "  --> remove package");
                        }
                        sModel.removePackage(Launcher.this, packageName);
                    }
                    // else, we are replacing the package, so a PACKAGE_ADDED will be sent
                    // later, we will update the package at this time
                } else {
                    if (!replacing) {
                        if (LauncherModel.DEBUG_LOADERS) {
                            d(LauncherModel.LOG_TAG, "  --> add package");
                        }
                        sModel.addPackage(Launcher.this, packageName);
                    } else {
                        if (LauncherModel.DEBUG_LOADERS) {
                            d(LauncherModel.LOG_TAG, "  --> update package " + packageName);
                        }
                        sModel.updatePackage(Launcher.this, packageName);
                        updateShortcutsForPackage(packageName);
                    }
                }
                removeDialog(DIALOG_CREATE_SHORTCUT);
            } else {
                if (LauncherModel.DEBUG_LOADERS) {
                    d(LauncherModel.LOG_TAG, "  --> sync package " + packageName);
                }
                sModel.syncPackage(Launcher.this, packageName);
            }
        }
    }

    /**
     * Receives notifications when applications are added/removed.
     */
    private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            closeSystemDialogs();
        }
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private class FavoritesChangeObserver extends ContentObserver {
        public FavoritesChangeObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onFavoritesChanged();
        }
    }

    /**
     * Receives notifications whenever the appwidgets are reset.
     */
    private class AppWidgetResetObserver extends ContentObserver {
        public AppWidgetResetObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange) {
            onAppWidgetReset();
        }
    }

    private static class DesktopBinder extends Handler implements MessageQueue.IdleHandler {
        static final int MESSAGE_BIND_ITEMS = 0x1;
        static final int MESSAGE_BIND_APPWIDGETS = 0x2;
        static final int MESSAGE_BIND_DRAWER = 0x3;

        // Number of items to bind in every pass
        static final int ITEMS_COUNT = 6;

        private final ArrayList<ItemInfo> mShortcuts;
        private final LinkedList<LauncherAppWidgetInfo> mAppWidgets;
        private final ApplicationsAdapter mDrawerAdapter;
        private final WeakReference<Launcher> mLauncher;

        public boolean mTerminate = false;

        DesktopBinder(Launcher launcher, ArrayList<ItemInfo> shortcuts,
                ArrayList<LauncherAppWidgetInfo> appWidgets, ApplicationsAdapter drawerAdapter) {

            mLauncher = new WeakReference<Launcher>(launcher);
            mShortcuts = shortcuts;
            mDrawerAdapter = drawerAdapter;

            // Sort widgets so active workspace is bound first
            final int currentScreen = launcher.mWorkspace.getCurrentScreen();
            final int screenNumber = launcher.mWorkspace.getChildCount();
            final int size = appWidgets.size();
            mAppWidgets = new LinkedList<LauncherAppWidgetInfo>();

            for (int i = 0; i < size; i++) {
                LauncherAppWidgetInfo appWidgetInfo = appWidgets.get(i);
                if (appWidgetInfo.screen >= screenNumber)
                    continue;
                if (appWidgetInfo.screen == currentScreen) {
                    mAppWidgets.addFirst(appWidgetInfo);
                } else {
                    mAppWidgets.addLast(appWidgetInfo);
                }
            }

            if (LauncherModel.DEBUG_LOADERS) {
                d(Launcher.LOG_TAG, "------> binding " + shortcuts.size() + " items");
                d(Launcher.LOG_TAG, "------> binding " + appWidgets.size() + " widgets");
            }
        }

        public void startBindingItems() {
            if (LauncherModel.DEBUG_LOADERS)
                d(Launcher.LOG_TAG, "------> start binding items");
            obtainMessage(MESSAGE_BIND_ITEMS, 0, mShortcuts.size()).sendToTarget();
        }

        public void startBindingDrawer() {
            obtainMessage(MESSAGE_BIND_DRAWER).sendToTarget();
        }

        public void startBindingAppWidgetsWhenIdle() {
            // Ask for notification when message queue becomes idle
            final MessageQueue messageQueue = Looper.myQueue();
            messageQueue.addIdleHandler(this);
        }

        public boolean queueIdle() {
            // Queue is idle, so start binding items
            startBindingAppWidgets();
            return false;
        }

        public void startBindingAppWidgets() {
            obtainMessage(MESSAGE_BIND_APPWIDGETS).sendToTarget();
        }

        @Override
        public void handleMessage(Message msg) {
            Launcher launcher = mLauncher.get();
            if (launcher == null || mTerminate) {
                return;
            }

            switch (msg.what) {
            case MESSAGE_BIND_ITEMS: {
                launcher.bindItems(this, mShortcuts, msg.arg1, msg.arg2);
                break;
            }
            case MESSAGE_BIND_DRAWER: {
                launcher.bindDrawer(this, mDrawerAdapter);
                break;
            }
            case MESSAGE_BIND_APPWIDGETS: {
                launcher.bindAppWidgets(this, mAppWidgets);
                break;
            }
            }
        }
    }
    
    static final int MSG_SCR_INDICATOR_DECAY = 0;

    Handler mScreenHandler = new ScreenHandler();

    class ScreenHandler extends Handler {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_SCR_INDICATOR_DECAY:
                if (mIndicator == null || mIndicator.getVisibility() != View.VISIBLE)
                    break;
                if (!keepIndicatorOn)
                    mIndicator.fadeOut();
                break;
            }
        }
    }

    /**
     * Show current screen on indicator, animate open if necessary
     * 
     * @param screen
     */
    void indicateScreen(int screen) {
        if (mIndicator == null)
            return;
        mIndicator.setScreen(screen, mWorkspace.getChildCount());
        showIndicator();

        mScreenHandler.removeMessages(MSG_SCR_INDICATOR_DECAY);
        mScreenHandler.sendEmptyMessageDelayed(MSG_SCR_INDICATOR_DECAY, 1200);
    }

    /**
     * 
     */
    void showIndicator() {
        if (keepIndicatorOn && !keepIndicatorOff) {
            mIndicator.setVisibility(View.VISIBLE);
            return;
        }

        if (mIndicator.getVisibility() != View.VISIBLE && !keepIndicatorOff) {
            mIndicator.fadeIn();
        }
    }
    
    protected boolean isEditMode(){
		return mIsEditMode;
	}
    
    protected void editWidget(final View widget){
        if(mWorkspace!=null){
            mIsWidgetEditMode=true;
            final CellLayout screen=(CellLayout) mWorkspace.getChildAt(mWorkspace.getCurrentScreen());
            if(screen!=null){
                mlauncherAppWidgetInfo = (LauncherAppWidgetInfo) widget.getTag();
                
                
                final Intent motosize = new Intent("com.motorola.blur.home.ACTION_SET_WIDGET_SIZE");
                final int appWidgetId = ((AppWidgetHostView)widget).getAppWidgetId();
                final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
                motosize.setComponent(appWidgetInfo.provider);
                motosize.putExtra("appWidgetId",appWidgetId );
                motosize.putExtra("com.motorola.blur.home.EXTRA_NEW_WIDGET", true);
                
                final int minw=(mWorkspace.getWidth()-screen.getLeftPadding()-screen.getRightPadding())/screen.getCountX();
                final int minh=(mWorkspace.getHeight()-screen.getBottomPadding()-screen.getTopPadding())/screen.getCountY();
                mScreensEditor=new ResizeViewHandler(this);
                // Create a default HightlightView if we found no face in the picture.
                int width = (mlauncherAppWidgetInfo.spanX*minw);
                int height = (mlauncherAppWidgetInfo.spanY*minh);

                final Rect screenRect = new Rect(screen.getLeftPadding(), screen.getTopPadding(), screen.getWidth()-screen.getLeftPadding()-screen.getRightPadding(), screen.getHeight()-screen.getTopPadding()-screen.getBottomPadding());
                final int x=mlauncherAppWidgetInfo.cellX*minw;
                final int y=mlauncherAppWidgetInfo.cellY*minh;
                final int[]spans=new int[]{1,1};
                final int[] position=new int[]{1,1};
                final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) widget.getLayoutParams();
                RectF widgetRect = new RectF(x,y, x + width, y + height);
                ((ResizeViewHandler)mScreensEditor).setup(null, screenRect, widgetRect, false,false,minw-10,minh-10);
                mDragLayer.addView(mScreensEditor);
                ((ResizeViewHandler)mScreensEditor).setOnValidateSizingRect(new ResizeViewHandler.OnSizeChangedListener() {

					@Override
					public void onTrigger(RectF r) {
						final float left = Math.round(r.left/minw) * minw;
						final float top = Math.round(r.top/minh) * minh;
						final float right = left + (Math.max(Math.round(r.width()/(minw)),1) * minw);
						final float bottom = top + (Math.max(Math.round(r.height()/(minh)),1) * minh);

						r.set(left, top, right, bottom);
					}
				});
                final Rect checkRect=new Rect();
                ((ResizeViewHandler)mScreensEditor).setOnSizeChangedListener(new ResizeViewHandler.OnSizeChangedListener() {
                    @Override
                    public void onTrigger(RectF r) {
                        int[]tmpspans={
                                Math.max(Math.round(r.width()/(minw)),1),
                                Math.max(Math.round(r.height()/(minh)),1)
                        };
                        int[] tmpposition={
                                Math.round(r.left/minw),
                                Math.round(r.top/minh)
                        };
                        checkRect.set(tmpposition[0],tmpposition[1],tmpposition[0]+tmpspans[0],tmpposition[1]+tmpspans[1]);
                        boolean ocupada=getModel().ocuppiedArea(screen.getScreen(), appWidgetId,checkRect);
                        if(!ocupada){
                            ((ResizeViewHandler)mScreensEditor).setColliding(false);
                        }else{
                            ((ResizeViewHandler)mScreensEditor).setColliding(true);
                        }
                        if(tmpposition[0]!=position[0] ||tmpposition[1]!=position[1]||
                                        tmpspans[0]!=spans[0]||tmpspans[1]!=spans[1]){
                            if(!ocupada){
                                position[0]=tmpposition[0];
                                position[1]=tmpposition[1];
                                spans[0]=tmpspans[0];
                                spans[1]=tmpspans[1];
                                lp.cellX = position[0];
                                lp.cellY = position[1];
                                lp.cellHSpan = spans[0];
                                lp.cellVSpan = spans[1];
                                widget.setLayoutParams(lp);
                                mlauncherAppWidgetInfo.cellX=lp.cellX;
                                mlauncherAppWidgetInfo.cellY=lp.cellY;
                                mlauncherAppWidgetInfo.spanX=lp.cellHSpan;
                                mlauncherAppWidgetInfo.spanY=lp.cellVSpan;
                                widget.setTag(mlauncherAppWidgetInfo);
                                //send the broadcast
                                motosize.putExtra("spanX", spans[0]);
                                motosize.putExtra("spanY", spans[1]);
                                Launcher.this.sendBroadcast(motosize);
                            }
                        }
                    }
                });
            }
        }
	}
    
	private void stopWidgetEdit(){
	    mIsWidgetEditMode=false;
	    if(mlauncherAppWidgetInfo!=null){
            LauncherModel.resizeItemInDatabase(this, mlauncherAppWidgetInfo,
                    LauncherSettings.Favorites.CONTAINER_DESKTOP,
                    mlauncherAppWidgetInfo.screen,
                    mlauncherAppWidgetInfo.cellX,
                    mlauncherAppWidgetInfo.cellY,
                    mlauncherAppWidgetInfo.spanX,
                    mlauncherAppWidgetInfo.spanY);
            mlauncherAppWidgetInfo=null;
	    }
	    //Remove the resizehandler view
        if(mScreensEditor!=null){
            mDragLayer.removeView(mScreensEditor);
            mScreensEditor=null;
        }
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// Ignore
	}

	public void fireSwipeUpAction(){
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
    	attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
    	getWindow().setAttributes(attrs);
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}
	
	public void fireSwipeDownAction() {
		WindowManager.LayoutParams attrs = getWindow().getAttributes();
    	attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	getWindow().setAttributes(attrs);
    	getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
	}
}