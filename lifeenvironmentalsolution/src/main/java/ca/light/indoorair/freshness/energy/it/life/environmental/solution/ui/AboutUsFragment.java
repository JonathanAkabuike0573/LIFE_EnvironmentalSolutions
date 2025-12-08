package ca.light.indoorair.freshness.energy.it.life.environmental.solution.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.light.indoorair.freshness.energy.it.life.environmental.solution.R;

public class AboutUsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about_us, container, false);

        WebView webView = view.findViewById(R.id.about_us_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (getContext() != null) {
                    SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE);
                    boolean isDarkMode = sharedPreferences.getBoolean("ThemeKey", false);
                    String theme = isDarkMode ? "dark" : "light";
                    view.evaluateJavascript("applyTheme('" + theme + "')", null);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("mailto:")) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse(url));


                    if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                        startActivity(emailIntent);
                    } else {

                        Toast.makeText(getContext(),
                                R.string.no_email_app_found_email_support_lifeenvironmentalsolution_com ,
                                Toast.LENGTH_LONG).show();
                    }
                    return true;
                }


                return false;
            }
        });

        webView.loadUrl(getString(R.string.file_android_asset_aboutpage_html));

        return view;
    }
}
