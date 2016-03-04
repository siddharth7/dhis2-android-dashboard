/*
 * Copyright (c) 2015, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.dashboard.ui.fragments.dashboard;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.squareup.otto.Subscribe;

import org.hisp.dhis.android.dashboard.DhisService;
import org.hisp.dhis.android.dashboard.R;
import org.hisp.dhis.android.dashboard.api.job.NetworkJob;
import org.hisp.dhis.android.dashboard.api.models.Access;
import org.hisp.dhis.android.dashboard.api.models.Dashboard;
import org.hisp.dhis.android.dashboard.api.models.Dashboard$Table;
import org.hisp.dhis.android.dashboard.api.models.meta.State;
import org.hisp.dhis.android.dashboard.api.network.SessionManager;
import org.hisp.dhis.android.dashboard.api.persistence.loaders.DbLoader;
import org.hisp.dhis.android.dashboard.api.persistence.loaders.Query;
import org.hisp.dhis.android.dashboard.api.persistence.preferences.ResourceType;
import org.hisp.dhis.android.dashboard.ui.adapters.DashboardAdapter;
import org.hisp.dhis.android.dashboard.ui.events.UiEvent;
import org.hisp.dhis.android.dashboard.ui.fragments.BaseFragment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class DashboardViewPagerFragment extends BaseFragment
        implements LoaderCallbacks<List<Dashboard>>, View.OnClickListener,
        ViewPager.OnPageChangeListener {

    static final String TAG = DashboardViewPagerFragment.class.getSimpleName();
    static final String IS_LOADING = "state:isLoading";
    static final int LOADER_ID = 1233432;
    private CoordinatorLayout coordinatorLayout;
    Snackbar snackbar;

    @Bind(R.id.dashboard_tabs)
    TabLayout mTabs;

    @Bind(R.id.dashboard_view_pager)
    ViewPager mViewPager;

    @Bind(R.id.toolbar)
    Toolbar mToolbar;

    @Bind(R.id.progress_bar)
    SmoothProgressBar mProgressBar;

    DashboardAdapter mDashboardAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        //return inflater.inflate(R.layout.fragment_dashboards, parent, false);
        View view = inflater.inflate(R.layout.fragment_dashboards,parent, false);
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ButterKnife.bind(this, view);

        mDashboardAdapter = new DashboardAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mDashboardAdapter);
        mViewPager.addOnPageChangeListener(this);

        mToolbar.setNavigationIcon(R.mipmap.ic_menu);
        mToolbar.setNavigationOnClickListener(this);
        mToolbar.setTitle(R.string.dashboard);
        mToolbar.inflateMenu(R.menu.menu_dashboard_fragment);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return onMenuItemClicked(item);
            }
        });

        if (isDhisServiceBound() &&
                !getDhisService().isJobRunning(DhisService.SYNC_DASHBOARDS) &&
                !SessionManager.getInstance().isResourceTypeSynced(ResourceType.DASHBOARDS)) {
            syncDashboards();
        }

        boolean isLoading = isDhisServiceBound() &&
                getDhisService().isJobRunning(DhisService.SYNC_DASHBOARDS);
        if ((savedInstanceState != null &&
                savedInstanceState.getBoolean(IS_LOADING)) || isLoading) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, savedInstanceState, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(IS_LOADING, mProgressBar
                .getVisibility() == View.VISIBLE);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<List<Dashboard>> onCreateLoader(int id, Bundle state) {
        if (id == LOADER_ID && isAdded()) {
            List<DbLoader.TrackedTable> trackedTables = Arrays.asList(
                    new DbLoader.TrackedTable(Dashboard.class));
            return new DbLoader<>(getActivity().getApplicationContext(),
                    trackedTables, new DashboardQuery());
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Dashboard>> loader, List<Dashboard> data) {
        if (loader.getId() == LOADER_ID && data != null) {
            setDashboards(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Dashboard>> loader) {
        if (loader.getId() == LOADER_ID) {
            setDashboards(null);
        }
    }

    @Override
    public void onClick(View view) {
        toggleNavigationDrawer();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        // stub implementation
    }

    @Override
    public void onPageSelected(int position) {
        Dashboard dashboard = mDashboardAdapter.getDashboard(position);
        Access dashboardAccess = dashboard.getAccess();

        Menu menu = mToolbar.getMenu();
        menu.findItem(R.id.add_dashboard_item)
                .setVisible(dashboardAccess.isUpdate());
        menu.findItem(R.id.manage_dashboard)
                .setVisible(dashboardAccess.isUpdate());
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // stub implementation
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onUiEventReceived(UiEvent uiEvent) {
        if (uiEvent.getEventType() == UiEvent.UiEventType.SYNC_DASHBOARDS) {
            boolean isLoading = isDhisServiceBound() &&
                    getDhisService().isJobRunning(DhisService.SYNC_DASHBOARDS);
            if (isLoading) {
                mProgressBar.setVisibility(View.VISIBLE);
            } else {
                mProgressBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void setDashboards(List<Dashboard> dashboards) {
        mDashboardAdapter.swapData(dashboards);
        mTabs.removeAllTabs();

        if (dashboards != null && !dashboards.isEmpty()) {
            mTabs.setupWithViewPager(mViewPager);
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connMgr = (ConnectivityManager) getActivity()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }
    public boolean onMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_dashboard_item: {
                long dashboardId = mDashboardAdapter
                        .getDashboard(mViewPager.getCurrentItem()).getId();
                DashboardItemAddFragment
                        .newInstance(dashboardId)
                        .show(getChildFragmentManager());
                return true;
            }
            case R.id.refresh: {
                if(isNetworkAvailable()) {
                    syncDashboards();
                }
                else
                {
                    Log.d("internet","nahi hai");
                    snackbar = Snackbar
                            .make(coordinatorLayout, "No Internet Connection!", Snackbar.LENGTH_LONG);
                    snackbar.setActionTextColor(Color.RED);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(Color.DKGRAY);
                    TextView textView = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                    textView.setTextColor(Color.YELLOW);
                    snackbar.show();
                }
                return true;
            }
            case R.id.add_dashboard: {
                new DashboardAddFragment()
                        .show(getChildFragmentManager());
                return true;
            }
            case R.id.manage_dashboard: {
                Dashboard dashboard = mDashboardAdapter
                        .getDashboard(mViewPager.getCurrentItem());
                DashboardManageFragment
                        .newInstance(dashboard.getId())
                        .show(getChildFragmentManager());
                return true;
            }
        }
        return false;
    }

    private void syncDashboards() {
        if (isDhisServiceBound()) {
            getDhisService().syncDashboardsAndContent();
            mProgressBar.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onResponseReceived(NetworkJob.NetworkJobResult<?> result) {
        if (result.getResourceType() == ResourceType.DASHBOARDS) {
            mProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private static class DashboardQuery implements Query<List<Dashboard>> {

        @Override
        public List<Dashboard> query(Context context) {
            List<Dashboard> dashboards = new Select()
                    .from(Dashboard.class)
                    .where(Condition.column(Dashboard$Table
                            .STATE).isNot(State.TO_DELETE.toString()))
                    .queryList();
            Collections.sort(dashboards, Dashboard.DISPLAY_NAME_COMPARATOR);
            return dashboards;
        }
    }
}
