package ca.light.indoorair.freshness.energy.it.life.environmental.solution.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.HashMap;
import java.util.Locale;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;
import ca.light.indoorair.freshness.energy.it.life.environmental.solution.data.FeedbackRepository;

public class FeedbackViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "FeedbackPrefs";
    private static final String LAST_SUBMISSION_TIMESTAMP = "last_submission_timestamp";
    private static final long TWENTY_FOUR_HOURS_MILLIS = 24 * 60 * 60 * 1000;

    private final FeedbackRepository feedbackRepository;
    private final SharedPreferences sharedPreferences;

    private final MutableLiveData<Boolean> _isSubmitting = new MutableLiveData<>(false);
    public final LiveData<Boolean> isSubmitting = _isSubmitting;

    private final MutableLiveData<Boolean> _submissionSuccess = new MutableLiveData<>(false);
    public final LiveData<Boolean> submissionSuccess = _submissionSuccess;

    private final MutableLiveData<String> _submissionError = new MutableLiveData<>();
    public final LiveData<String> submissionError = _submissionError;

    private final MutableLiveData<String> _timerText = new MutableLiveData<>();
    public final LiveData<String> timerText = _timerText;

    private final MutableLiveData<Boolean> _isSendButtonEnabled = new MutableLiveData<>(true);
    public final LiveData<Boolean> isSendButtonEnabled = _isSendButtonEnabled;

    private CountDownTimer countDownTimer;

    public FeedbackViewModel(@NonNull Application application) {
        super(application);
        feedbackRepository = new FeedbackRepository();
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void checkSubmissionCooldown() {
        long lastSubmissionTime = sharedPreferences.getLong(LAST_SUBMISSION_TIMESTAMP, 0);
        long currentTime = System.currentTimeMillis();
        long timeDifference = currentTime - lastSubmissionTime;

        if (timeDifference < TWENTY_FOUR_HOURS_MILLIS) {
            long remainingTime = TWENTY_FOUR_HOURS_MILLIS - timeDifference;
            startTimer(remainingTime);
        } else {
            _isSendButtonEnabled.setValue(true);
            _timerText.setValue(null); // No timer text
        }
    }

    private void startTimer(long duration) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        _isSendButtonEnabled.setValue(false);

        countDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long hours = (millisUntilFinished / (1000 * 60 * 60)) % 24;
                long minutes = (millisUntilFinished / (1000 * 60)) % 60;
                long seconds = (millisUntilFinished / 1000) % 60;
                _timerText.setValue(String.format(Locale.getDefault(), getApplication().getString(R.string.you_can_submit_again_in_02d_02d_02d), hours, minutes, seconds));
            }

            @Override
            public void onFinish() {
                _timerText.setValue(null);
                _isSendButtonEnabled.setValue(true);
            }
        }.start();
    }

    public void submitFeedback(String name, String email, String phone, String feedback, float rating) {
        _isSubmitting.setValue(true);

        HashMap<String, Object> feedbackMap = new HashMap<>();
        feedbackMap.put( getApplication().getString(R.string.name) , name);
        feedbackMap.put( getApplication().getString(R.string.email), email);
        feedbackMap.put(getApplication().getString(R.string.phone), phone);
        feedbackMap.put(getApplication().getString(R.string.devicemodel), Build.MANUFACTURER + " " + Build.MODEL);
        feedbackMap.put(getApplication().getString(R.string.rating), rating);
        feedbackMap.put(getApplication().getString(R.string.message), feedback);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            feedbackRepository.submitFeedback(feedbackMap)
                    .addOnCompleteListener(task -> {
                        _isSubmitting.setValue(false);
                        if (task.isSuccessful()) {
                            _submissionSuccess.setValue(true);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putLong(LAST_SUBMISSION_TIMESTAMP, System.currentTimeMillis());
                            editor.apply();
                            startTimer(TWENTY_FOUR_HOURS_MILLIS);
                        } else {
                            _submissionError.setValue( getApplication().getString(R.string.failed_to_submit_feedback_please_check_your_network_connection_and_try_again));
                        }
                    });
        }, 5000); // 5-second delay
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
