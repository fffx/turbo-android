package com.basecamp.turbolinks

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI

abstract class TurbolinksFragment : Fragment() {
    lateinit var location: String
    lateinit var sessionName: String
    lateinit var router: TurbolinksRouter
    lateinit var session: TurbolinksSession
    lateinit var navigator: TurbolinksNavigator

    lateinit var sharedViewModel: TurbolinksSharedViewModel
    lateinit var pageViewModel: TurbolinksFragmentViewModel

    var navigatedFromModalResult: Boolean = false
    open val displaysToolbarTitle: Boolean = true
    abstract val toolbar: Toolbar?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        location = currentLocation()
        sessionName = currentSessionName()
        sharedViewModel = TurbolinksSharedViewModel.get(requireActivity())
        pageViewModel = TurbolinksFragmentViewModel.get(location, this)

        observeLiveData()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireNotNull(context as? TurbolinksActivity) {
            "The fragment Activity must implement TurbolinksActivity"
        }

        router = activity.delegate.router
        session = activity.delegate.getSession(sessionName)
        navigator = TurbolinksNavigator(this, session, router)
    }

    override fun onStart() {
        super.onStart()

        navigatedFromModalResult = sharedViewModel.modalResult?.let {
            navigator.navigate(it.location, it.action)
        } ?: false
    }

    protected open fun initToolbar() {
        toolbar?.let {
            NavigationUI.setupWithNavController(it, findNavController())
            it.setNavigationOnClickListener { navigateUp() }
        }
    }

    // ----------------------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------------------

    fun navigate(location: String, action: String = "advance"): Boolean {
        return navigator.navigate(location, action)
    }

    fun navigateUp(): Boolean {
        return navigator.navigateUp()
    }

    fun navigateBack() {
        navigator.navigateBack()
    }

    fun clearBackStack() {
        navigator.clearBackStack()
    }

    // ----------------------------------------------------------------------------
    // Private
    // ----------------------------------------------------------------------------

    private fun observeLiveData() {
        if (displaysToolbarTitle) {
            pageViewModel.title.observe(this, Observer {
                toolbar?.title = it
            })
        }
    }

    private fun currentLocation(): String {
        return requireNotNull(arguments?.getString("location")) {
            "A location argument must be provided"
        }
    }

    private fun currentSessionName(): String {
        return requireNotNull(arguments?.getString("sessionName")) {
            "A sessionName argument must be provided"
        }
    }
}
