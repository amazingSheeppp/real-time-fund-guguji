package com.fund.guguji.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fund.guguji.R;
import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.data.api.AuthSession;
import com.fund.guguji.data.api.GugujiApi;
import com.fund.guguji.data.api.model.AuthModels;

import java.util.Locale;
import java.util.regex.Pattern;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 邮箱验证码登录页(墨水屏风格)
 * 未注册邮箱验证后自动注册并登录;登录态写入 AuthSession。
 */
public class LoginActivity extends AppCompatActivity {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CompositeDisposable disposables = new CompositeDisposable();

    private GugujiApi gugujiApi;
    private AuthSession authSession;

    private EditText editEmail;
    private EditText editCode;
    private EditText editPassword;
    private TextView btnSendCode;
    private TextView btnSubmit;

    // 倒计时状态(true=冷却中)
    private boolean countdownActive = false;
    private int countdownSeconds = 0;
    private final android.os.Handler countdownHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable countdownRunnable = new Runnable() {
        @Override
        public void run() {
            if (!countdownActive) return;
            countdownSeconds--;
            if (countdownSeconds <= 0) {
                countdownActive = false;
                updateSendCodeButton();
                return;
            }
            updateSendCodeButton();
            countdownHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        RealTimeFundApp app = (RealTimeFundApp) getApplication();
        gugujiApi = app.getGugujiApi();
        authSession = app.getAuthSession();

        editEmail = findViewById(R.id.edit_email);
        editCode = findViewById(R.id.edit_code);
        editPassword = findViewById(R.id.edit_password);
        btnSendCode = findViewById(R.id.btn_send_code);
        btnSubmit = findViewById(R.id.btn_submit);

        btnSendCode.setOnClickListener(v -> requestSendCode());
        btnSubmit.setOnClickListener(v -> submitLogin());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        if (authSession.isLoggedIn()) {
            editEmail.setText(authSession.getEmail());
        }
    }

    /** 发送验证码 */
    private void requestSendCode() {
        if (countdownActive) return;

        String email = editEmail.getText().toString().trim();
        if (!isValidEmail(email)) {
            Toast.makeText(this, R.string.login_email_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText(R.string.login_sending);

        disposables.add(
                gugujiApi.sendCode(email)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                resp -> {
                                    Toast.makeText(this, R.string.login_code_sent, Toast.LENGTH_SHORT).show();
                                    startCountdown(resp != null ? resp.getResendIntervalSeconds() : 60);
                                },
                                throwable -> {
                                    btnSendCode.setEnabled(true);
                                    btnSendCode.setText(R.string.login_send_code);
                                    Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                        )
        );
    }

    /** 验证码登录/注册 */
    private void submitLogin() {
        String email = editEmail.getText().toString().trim();
        String code = editCode.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (!isValidEmail(email)) {
            Toast.makeText(this, R.string.login_email_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        if (code.length() != 6) {
            Toast.makeText(this, R.string.login_code_invalid, Toast.LENGTH_SHORT).show();
            return;
        }
        // 密码为选填,一旦填写需满足后端 6-32 位校验
        if (!TextUtils.isEmpty(password) && password.length() < 6) {
            Toast.makeText(this, R.string.login_password_short, Toast.LENGTH_SHORT).show();
            return;
        }

        setSubmitting(true);

        String finalPassword = TextUtils.isEmpty(password) ? null : password;
        disposables.add(
                loginFlow(email, code, finalPassword)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                token -> {
                                    setSubmitting(false);
                                    persistSession(token);
                                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                },
                                throwable -> {
                                    setSubmitting(false);
                                    Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                        )
        );
    }

    /** 登录成功后持久化会话;刷新令牌为空时尝试用返回的 access token 兜底 */
    private void persistSession(AuthModels.TokenResponse token) {
        String refreshToken = token.getRefreshToken();
        if (refreshToken == null && token.getAccessToken() != null) {
            refreshToken = token.getAccessToken();
        }
        long userId = token.getUser() != null ? token.getUser().getId() : -1;
        String email = token.getUser() != null ? token.getUser().getEmail() : editEmail.getText().toString().trim();
        String nickname = token.getUser() != null ? token.getUser().getNickname() : "";
        authSession.saveSession(token.getAccessToken(), refreshToken, userId, email, nickname);
    }

    private Observable<AuthModels.TokenResponse> loginFlow(String email, String code, String password) {
        Observable<AuthModels.TokenResponse> flow = gugujiApi.loginByCode(email, code, password);
        if (!TextUtils.isEmpty(password)) {
            // 设置密码语义下,验证码失效或错误时回退尝试账号密码登录,让已有密码用户也能进入
            flow = flow.onErrorResumeNext(throwable ->
                    gugujiApi.loginByPassword(email, password));
        }
        return flow;
    }

    private void startCountdown(int seconds) {
        countdownActive = true;
        countdownSeconds = Math.max(1, seconds);
        countdownHandler.removeCallbacks(countdownRunnable);
        updateSendCodeButton();
        countdownHandler.postDelayed(countdownRunnable, 1000);
    }

    private void updateSendCodeButton() {
        if (countdownActive) {
            btnSendCode.setEnabled(false);
            btnSendCode.setText(String.format(Locale.getDefault(),
                    getString(R.string.login_resend_code), countdownSeconds));
        } else {
            btnSendCode.setEnabled(true);
            btnSendCode.setText(R.string.login_send_code);
        }
    }

    private void setSubmitting(boolean submitting) {
        btnSubmit.setEnabled(!submitting);
        btnSubmit.setText(submitting ? "登录中…" : getString(R.string.login_submit));
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismantleCountdown();
        disposables.clear();
    }

    private void dismantleCountdown() {
        countdownActive = false;
        countdownHandler.removeCallbacks(countdownRunnable);
    }
}