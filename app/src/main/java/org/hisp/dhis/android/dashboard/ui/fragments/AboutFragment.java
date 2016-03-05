package org.hisp.dhis.android.dashboard.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.hisp.dhis.android.dashboard.BuildConfig;
import org.hisp.dhis.android.dashboard.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by siddharthsingh on 05/03/16.
 */
public class AboutFragment extends BaseFragment {
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    TextView mVersionNumber,mWebsite,mEmail;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        ButterKnife.bind(this, view);
        mToolbar.setNavigationIcon(R.mipmap.ic_menu);
        mToolbar.setTitle(R.string.about);
        mVersionNumber=(TextView) view.findViewById(R.id.version_textview);
        mWebsite=(TextView) view.findViewById(R.id.website_textview);
        mEmail=(TextView) view.findViewById(R.id.email_textview);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavigationDrawer();
            }
        });
        mVersionNumber.setText("version" + " " + BuildConfig.VERSION_NAME);
        mWebsite.setClickable(true);
        mWebsite.setMovementMethod(LinkMovementMethod.getInstance());
        String text = "<a href='https://www.dhis2.org'> Visit Website </a>";
        mWebsite.setText(Html.fromHtml(text));
        mEmail.setText(Html.fromHtml("<a href=\"mailto:dhis2mobileteam@gmail.com\">Send Feedback</a>"));
        mEmail.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @OnClick(R.id.check_for_update_button)
    @SuppressWarnings("unused")
    public void onClick() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=org.hisp.dhis.android.dashboard"));
        startActivity(intent);
    }
}
