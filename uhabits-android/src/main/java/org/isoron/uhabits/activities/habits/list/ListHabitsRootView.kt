/*
 * Copyright (C) 2016 Álinson Santos Xavier <isoron@gmail.com>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.habits.list

import android.content.*
import android.os.Build.VERSION.*
import android.os.Build.VERSION_CODES.*
import android.support.v7.widget.Toolbar
import android.view.*
import android.view.ViewGroup.LayoutParams.*
import android.widget.*
import org.isoron.androidbase.activities.*
import org.isoron.uhabits.*
import org.isoron.uhabits.activities.common.views.*
import org.isoron.uhabits.activities.habits.list.views.*
import org.isoron.uhabits.core.models.*
import org.isoron.uhabits.core.preferences.*
import org.isoron.uhabits.core.tasks.*
import org.isoron.uhabits.core.ui.screens.habits.list.*
import org.isoron.uhabits.core.utils.*
import org.isoron.uhabits.utils.*
import java.lang.Math.*
import javax.inject.*

const val MAX_CHECKMARK_COUNT = 60

@ActivityScope
class ListHabitsRootView @Inject constructor(
        @ActivityContext context: Context,
        hintListFactory: HintListFactory,
        preferences: Preferences,
        midnightTimer: MidnightTimer,
        runner: TaskRunner,
        private val listAdapter: HabitCardListAdapter
) : BaseRootView(context), ModelObservable.Listener {
    val listView: HabitCardListView
    val llEmpty: ViewGroup
    val tbar: Toolbar
    val progressBar: ProgressBar
    val hintView: HintView
    val header: HeaderView

    init {
        tbar = buildToolbar()
        header = HeaderView(context, preferences, midnightTimer)
        listView = HabitCardListView(context, listAdapter)
        llEmpty = EmptyListView(context)
        progressBar = TaskProgressBar(context, runner)
        val hints = resources.getStringArray(R.array.hints)
        val hintList = hintListFactory.create(hints)
        hintView = HintView(context, hintList)

        addView(RelativeLayout(context).apply {
            background = sres.getDrawable(R.attr.windowBackgroundColor)
            addAtTop(tbar)
            addBelow(header, tbar)
            addBelow(listView, header, height = MATCH_PARENT)
            addBelow(llEmpty, header, height = MATCH_PARENT)
            addBelow(progressBar, header) {
                it.topMargin = dp(-6.0f).toInt()
            }
            addAtBottom(hintView)
            if (SDK_INT < LOLLIPOP) {
                addBelow(ShadowView(context), tbar)
                addBelow(ShadowView(context), header)
            }
        }, MATCH_PARENT, MATCH_PARENT)

        listAdapter.setListView(listView)
        initToolbar()
    }

    override fun getToolbar(): Toolbar {
        return tbar
    }

    override fun onModelChange() {
        updateEmptyView()
    }

    fun setController(controller: ListHabitsController,
                      menu: ListHabitsSelectionMenu) {

        val listController = HabitCardListController(listAdapter)
        listController.setHabitListener(controller)
        listController.setSelectionListener(menu)
        listView.setController(listController)
        menu.setListController(listController)
        header.setScrollController(object : ScrollableChart.ScrollController {
            override fun onDataOffsetChanged(newDataOffset: Int) {
                listView.setDataOffset(newDataOffset)
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        listAdapter.observable.addListener(this)
    }

    override fun onDetachedFromWindow() {
        listAdapter.observable.removeListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val count = getCheckmarkCount()
        header.setButtonCount(count)
        header.setMaxDataOffset(max(MAX_CHECKMARK_COUNT - count, 0))
        listView.setCheckmarkCount(count)
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun getCheckmarkCount(): Int {
        val nameWidth = dim(R.dimen.habitNameWidth)
        val buttonWidth = dim(R.dimen.checkmarkWidth)
        val labelWidth = max((measuredWidth / 3).toFloat(), nameWidth)
        val buttonCount = ((measuredWidth - labelWidth) / buttonWidth).toInt()
        return min(MAX_CHECKMARK_COUNT, max(0, buttonCount))
    }

    private fun updateEmptyView() {
        llEmpty.visibility = when (listAdapter.itemCount) {
            0 -> VISIBLE
            else -> GONE
        }
    }
}