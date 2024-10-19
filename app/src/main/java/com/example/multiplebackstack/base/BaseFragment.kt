package com.example.multiplebackstack.base

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.multiplebackstack.bottomnavigation.BottomNavigator
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem

abstract class BaseFragment(layoutId: Int) : Fragment(layoutId), BottomNavigator,
    ScrollableFragment {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(this::class.simpleName, "onAttach - instance: ${hashCode()}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(this::class.simpleName, "onCreate - instance: ${hashCode()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(this::class.simpleName, "onCreateView - instance: ${hashCode()}")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(
            this::class.simpleName,
            "onViewCreated - instance: ${hashCode()} - savedInstanceState: $savedInstanceState"
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(this::class.simpleName, "onStart - instance: ${hashCode()}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(this::class.simpleName, "onResume - instance: ${hashCode()}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(this::class.simpleName, "onPause - instance: ${hashCode()}")
    }

    override fun onStop() {
        super.onStop()
        Log.d(this::class.simpleName, "onStop - instance: ${hashCode()}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(this::class.simpleName, "onDestroyView - instance: ${hashCode()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(this::class.simpleName, "onDestroy - instance: ${hashCode()}")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(this::class.simpleName, "onDetach - instance: ${hashCode()}")
    }

    override fun navigate(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        (activity as? BottomNavigator)?.navigate(bottomNavigableItem, args)
    }

    override fun navigate(identifier: String, args: Bundle?) {
        (activity as? BottomNavigator)?.navigate(identifier, args)
    }

    override fun scrollToTop() {
        // Default implementation
    }
}