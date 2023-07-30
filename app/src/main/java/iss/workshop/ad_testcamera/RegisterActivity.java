package iss.workshop.ad_testcamera;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{
    private EditText usernamebox, passwordbox, passwordconfirm;
    private TextView usererr, passworderr, generalerr, loginlink;
    private TextWatcher usertextWatcher, passwordtextWatcher;
    private AppCompatButton registerbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernamebox = findViewById(R.id.usernamebox);
        passwordbox = findViewById(R.id.passwordbox);
        passwordconfirm = findViewById(R.id.passwordconfirm);
        usererr = findViewById(R.id.usernameerr);
        passworderr = findViewById(R.id.passworderr);
        generalerr = findViewById(R.id.generalerr);

        usertextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                usererr.setVisibility(View.GONE);
                generalerr.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        passwordtextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                passworderr.setVisibility(View.GONE);
                generalerr.setVisibility(View.GONE);
                String passwordone = passwordbox.getText().toString();
                String passwordtwo = passwordconfirm.getText().toString();

                if(passwordone.equals(passwordtwo)){
                    passworderr.setVisibility(View.GONE);
                }
                else{
                    passworderr.setText(R.string.PASSWORD_MISMATCH_ERR);
                    passworderr.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        usernamebox.addTextChangedListener(usertextWatcher);
        passwordbox.addTextChangedListener(passwordtextWatcher);
        passwordconfirm.addTextChangedListener(passwordtextWatcher);

        registerbtn = findViewById(R.id.registerbtn);
        registerbtn.setOnClickListener(this);
        loginlink = findViewById(R.id.loginlink);
        loginlink.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if(id == R.id.registerbtn){
            String username = usernamebox.getText().toString();
            String passwordone = passwordbox.getText().toString();
            String passwordtwo = passwordconfirm.getText().toString();
            if(passwordone.equals(passwordtwo)){
                if(username != null && !username.isEmpty() && passwordone != null && !passwordone.isEmpty() && passwordtwo != null && !passwordtwo.isEmpty()){
                    registerUser(username, passwordone);
                }
                else{
                    generalerr.setText(R.string.LOGINBLANK_ERR);
                    generalerr.setVisibility(View.VISIBLE);
                }
            }
        }
        else if(id == R.id.loginlink){
            Intent launchLogin = new Intent(this, LoginActivity.class);
            startActivity(launchLogin);
        }
    }

    public void registerUser(String username, String password){
        try{
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .writeTimeout(10, TimeUnit.SECONDS).build();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("userID", username)
                    .addFormDataPart("password", password)
                    .build();

            Request request = new Request.Builder()
                    .url(Constants.REGISTER_API)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    //Timeout Error
                    runOnUiThread(() -> {
                        generalerr.setText(getString(R.string.SERVER_DOWN_ERR));
                        generalerr.setVisibility(View.VISIBLE);});
                }

                public void onResponse(Call call, final Response response) throws IOException {
                    if(response.code() == 200){
                        String username = response.body().string();
                        runOnUiThread(() -> launchHome(username));
                    }
                    else if(response.code() == 400){
                        String errors = response.body().string();
                        try {
                            String error;
                            JSONObject object = new JSONObject(errors);
                            if(object.has("userID")){
                                error = object.getString("userID");
                                runOnUiThread(() -> {
                                    usererr.setText(error);
                                    usererr.setVisibility(View.VISIBLE);
                                });
                            }
                            else if(object.has("password")){
                                error = object.getString("password");
                                runOnUiThread(() -> {
                                    passworderr.setText(error);
                                    passworderr.setVisibility(View.VISIBLE);
                                });
                            }
                            else if(object.has("general")){
                                error = object.getString("general");
                                runOnUiThread(() -> showError(error));
                            }
                            else{
                                runOnUiThread(() -> showError(null));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    else{
                        runOnUiThread(() -> showError(null));
                    }
                }
            });
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void launchHome(String username){
        new AlertDialog.Builder(RegisterActivity.this)
                .setTitle("Registration Successful!")
                .setMessage(getString(R.string.welcome_register_msg))
                .setPositiveButton("Proceed", (dialogInterface, i) -> {
                    Intent launchHome = new Intent(this, HomeActivity.class);
                    launchHome.putExtra("username", username);
                    startActivity(launchHome);
                    finish();
                })
                .show();
    }

    public void showError(String error){
        if(error == null){
            error = getResources().getString(R.string.GENERIC_ERR);
        }

        generalerr.setText(error);
        generalerr.setVisibility(View.VISIBLE);
    }
}