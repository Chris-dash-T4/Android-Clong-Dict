package edu.cmu.androidstuco.clongdict.data;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import edu.cmu.androidstuco.clongdict.data.model.LoggedInUser;

import java.io.IOException;

import javax.security.auth.login.LoginException;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    public Result<LoggedInUser> login(String username, String password) {

        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            LoggedInUser fakeUser =
                    new LoggedInUser(
                            java.util.UUID.randomUUID().toString(),
                            "Jane Doe");
            auth.signInWithEmailAndPassword(username,password)
                    /*
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                fakeUser = new LoggedInUser(auth.getCurrentUser().getUid(),auth.getCurrentUser().getDisplayName())
                            }
                            else {
                                throw new LoginException();
                            }
                        }
                    })*/;
            // TODO: handle loggedInUser authentication
            return new Result.Success<>(fakeUser);
        } catch (Exception e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}