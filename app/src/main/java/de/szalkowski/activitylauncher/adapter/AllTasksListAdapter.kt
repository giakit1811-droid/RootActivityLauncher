package de.szalkowski.activitylauncher.adapter

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.szalkowski.activitylauncher.R
import de.szalkowski.activitylauncher.info.MyActivityInfo
import de.szalkowski.activitylauncher.info.MyPackageInfo
import de.szalkowski.activitylauncher.info.PackageManagerCache
import de.szalkowski.activitylauncher.info.PackageManagerCache.Companion.getPackageManagerCache
import de.szalkowski.activitylauncher.provider.AsyncProvider
import java.util.*

class AllTasksListAdapter(private var context: Context, updater: AsyncProvider<AllTasksListAdapter?>.Updater?) :
    BaseExpandableListAdapter(), Filterable {

    private val pm: PackageManager? = null
    private var packages: MutableList<MyPackageInfo>? = null
    private var filtered: List<MyPackageView>? =
        null

    private class MyPackageView internal constructor(var parent: MyPackageInfo, var id: Int) {
        private inner class Child {
            var child: MyActivityInfo? = null
            var id: Long = 0
        }

        var children = ArrayList<Child>()
        fun add(activity: MyActivityInfo?, id: Long) {
            val child = Child()
            child.child = activity
            child.id = id
            children.add(child)
        }

    }


    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return packages!![groupPosition].getActivity(childPosition)!!
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val activity = getChild(groupPosition, childPosition) as MyActivityInfo
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.all_activities_child_item, null)
        val text1 = view.findViewById<View>(android.R.id.text1) as TextView
        val pm = context.packageManager
        try {
            val info = pm.getActivityInfo(activity.componentName, 0)
            val sb = StringBuilder()
            sb.append(activity.name)
            if (!info.isEnabled || !info.exported) sb.append(" (Root only)")
            text1.text = sb.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            text1.text = activity.name
            e.printStackTrace()
        }
        val text2 = view.findViewById<View>(android.R.id.text2) as TextView
        text2.text = activity.componentName.className
        val icon =
            view.findViewById<View>(android.R.id.icon) as ImageView
        icon.setImageDrawable(activity.icon)
        return view
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return packages!![groupPosition].activitiesCount
    }

    override fun getGroup(groupPosition: Int): Any {
        return packages!![groupPosition]
    }

    override fun getGroupCount(): Int {
        return packages!!.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup
    ): View {
        val pack = getGroup(groupPosition) as MyPackageInfo
        val inflater =
            context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.all_activities_group_item, null)
        val text = view.findViewById<View>(android.R.id.text1) as TextView
        text.text = pack.name
        val icon =
            view.findViewById<View>(android.R.id.icon) as ImageView
        icon.setImageDrawable(pack.abstractIcon)
        return view
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun resolve(updater: AsyncProvider<AllTasksListAdapter?>.Updater) {
        val cache: PackageManagerCache? = getPackageManagerCache(this.pm!!)
        val all_packages: List<PackageInfo> = this.pm.getInstalledPackages(0)
        packages = ArrayList(all_packages.size)
        updater.updateMax(all_packages.size)
        updater.update(0)
        for (i in all_packages.indices) {
            updater.update(i + 1)
            val pack = all_packages[i]
            var mypack: MyPackageInfo
            try {
                mypack = cache?.getPackageInfo(pack.packageName)!!
                if (mypack.activitiesCount > 0) {
                    packages?.add(mypack)
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            } catch (ignored: RuntimeException) {
            }
        }
        Collections.sort(packages)
        this.filtered = createFilterView("")
    }

    private fun createFilterView(query: String): List<MyPackageView> {
        val q = query.toLowerCase()
        val result = ArrayList<MyPackageView>()
        for (j in packages!!.indices) {
            val parent = packages!![j]
            val entry = MyPackageView(parent, j)
            for (i in 0 until parent.activitiesCount) {
                val child = parent.getActivity(i)
                if (child!!.name!!.toLowerCase().contains(q) ||
                    child.componentName.flattenToString().toLowerCase()
                        .contains(q) || child.iconResourceName != null && child.iconResourceName!!.toLowerCase()
                        .contains(q)
                ) {
                    entry.add(child, i.toLong())
                }
            }
            if (!entry.children.isEmpty() ||
                parent.name!!.toLowerCase().contains(q) ||
                parent.packageName.toLowerCase()
                    .contains(q) || parent.iconResourceName != null && parent.iconResourceName!!.contains(
                    q
                )
            ) {
                result.add(entry)
            }
        }
        return result
    }


    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val result: List<MyPackageView> =
                    createFilterView(constraint.toString())
                val wrapped = FilterResults()
                wrapped.values = result
                wrapped.count = result.size
                return wrapped
            }

            override fun publishResults(
                constraint: CharSequence,
                results: FilterResults
            ) {
                if (results != null) {
                    filtered =
                        results.values as List<MyPackageView>
                    notifyDataSetChanged()
                }
            }
        }
    }


    init {
        val pm = context.packageManager
        val cache = getPackageManagerCache(pm)
        val allPackages = pm.getInstalledPackages(0)
        packages = ArrayList(allPackages.size)
        updater?.updateMax(allPackages.size)
        updater?.update(0)
        for (i in allPackages.indices) {
            updater?.update(i + 1)
            val pack = allPackages[i]
            var mypack: MyPackageInfo?
            try {
                mypack = cache!!.getPackageInfo(pack.packageName)
                if (mypack!!.activitiesCount > 0) {
                    (packages as ArrayList<MyPackageInfo>).add(mypack!!)
                }
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
        }
        (packages as ArrayList<MyPackageInfo>).sort()
    }
}