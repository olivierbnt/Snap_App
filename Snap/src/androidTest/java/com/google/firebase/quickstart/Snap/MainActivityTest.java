package com.google.firebase.quickstart.Snap;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.Intents;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intending;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.startsWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private static final String TAG = "MainActivityTest";

    private ServiceIdlingResource mUploadIdlingResource;

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @BeforeClass
    public static void grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = getTargetContext().getPackageName();
            String testPackageName = packageName + ".test";

            // Grant "WRITE_EXTERNAL_STORAGE"
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + packageName + Manifest.permission.WRITE_EXTERNAL_STORAGE);
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + testPackageName + Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @Before
    public void before() {
        // Initialize intents
        Intents.init();

        // Idling resource
        mUploadIdlingResource = new ServiceIdlingResource(mActivityTestRule.getActivity(),
                MyUploadService.class);
        Espresso.registerIdlingResources(mUploadIdlingResource);
    }

    @After
    public void after() {
        // Release intents
        Intents.release();

        // Idling resource
        if (mUploadIdlingResource != null) {
            Espresso.unregisterIdlingResources(mUploadIdlingResource);
        }
    }


    @Test
    public void uploadPhotoTest() throws InterruptedException {
        // Log out to start
        logOutIfPossible();

        // Create a temp file
        createTempFile();

        // Click sign in
        ViewInteraction signInButton = onView(
                allOf(withId(R.id.button_sign_in), withText(R.string.sign_in_anonymously),
                        isDisplayed()));
        signInButton.perform(click());

        // Wait for sign in
        Thread.sleep(5000);

        // Click upload
        ViewInteraction uploadButton = onView(
                allOf(withId(R.id.button_camera), withText(R.string.camera_button_text),
                        isDisplayed()));
        uploadButton.perform(click());

        // Confirm that download link label is displayed
        onView(withText(R.string.label_link))
                .check(matches(isDisplayed()));

        // Confirm that there is a download link on screen
        onView(withId(R.id.picture_download_uri))
                .check(matches(withText(startsWith("https://firebasestorage.googleapis.com"))));

        // Click download
        ViewInteraction downloadButton = onView(
                allOf(withId(R.id.button_download), withText(R.string.download),
                        isDisplayed()));
        downloadButton.perform(click());

        // Wait for download
        Thread.sleep(5000);

        // Confirm that a success dialog appears
        onView(withText(R.string.success)).inRoot(isDialog())
                .check(matches(isDisplayed()));
    }

    /**
     * Create a file to be selected by tests.
     */
    private void createTempFile() {
        // Create fake RESULT_OK Intent
        Intent intent = new Intent();
        intent.putExtra("is-espresso-test", true);

        // Create a temporary file for the result of the intent
        File external = mActivityTestRule.getActivity().getExternalFilesDir(null);
        File imageFile = new File(external, "tmp.jpg");
        try {
            imageFile.createNewFile();
        } catch (IOException e) {
            Log.e(TAG, "createNewFile", e);
        }
        intent.setData(Uri.fromFile(imageFile));

        Instrumentation.ActivityResult result = new Instrumentation.ActivityResult(
                Activity.RESULT_OK, intent);

        // Intercept photo intent
        Matcher<Intent> pictureIntentMatch = allOf(hasAction(Intent.ACTION_GET_CONTENT));
        intending(pictureIntentMatch).respondWith(result);
    }

    /**
     * Click the 'Log Out' overflow menu if it exists (which would mean we're signed in).
     */
    private void logOutIfPossible() {
        try {
            openActionBarOverflowOrOptionsMenu(getTargetContext());
            onView(withText(R.string.log_out)).perform(click());
        } catch (NoMatchingViewException e) {
            // Ignore exception since we only want to do this operation if it's easy.
        }

    }
}
