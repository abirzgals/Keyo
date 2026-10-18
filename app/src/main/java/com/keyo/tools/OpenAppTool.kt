package com.keyo.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import org.json.JSONObject

class OpenAppTool : Tool {
    override val name = "open_app"
    override val uiLabel = "📱 Open app"
    override val uiExample = "Open Telegram"
    override val description = "Open an app by name. Searches all installed apps on the device. Accepts app name in any language (Russian, English, etc)."
    override val parameters = JSONObject("""
        {
            "type": "object",
            "properties": {
                "app_name": {
                    "type": "string",
                    "description": "App name to open (e.g. 'Telegram', 'Slack', 'Camera')"
                }
            },
            "required": ["app_name"]
        }
    """)

    override suspend fun execute(context: Context, args: JSONObject): ToolResult {
        val query = args.getString("app_name").lowercase().trim()

        val pm = context.packageManager
        // Tools run on Main; enumerating every launcher activity and loading each one's label is
        // a resource load from a foreign APK per app — hundreds of ms on a phone with 150 apps,
        // on the IME's main thread. So the whole match and launch run on IO.
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { match(context, pm, query) }
    }

    private fun match(context: Context, pm: PackageManager, query: String): ToolResult {
        // Each label is loaded once: the passes below used to load all of them up to four times.
        val apps = getLaunchableApps(pm).map { it to it.loadLabel(pm).toString() }
        fun ResolveInfo.go() = launchApp(context, pm, this)

        // 1. Exact match on label
        apps.find { it.second.lowercase() == query }?.let { return it.first.go() }

        // 2. Contains match
        val contains = apps.filter { it.second.lowercase().contains(query) }
        if (contains.size == 1) return contains[0].first.go()

        // 3. Package name contains match
        apps.find { it.first.activityInfo.packageName.lowercase().contains(query) }?.let { return it.first.go() }

        // 4. Fuzzy — try removing common suffixes/prefixes
        val simplified = query.replace("the app ", "").replace("app ", "").trim()
        val fuzzy = apps.filter {
            val label = it.second.lowercase()
            label.contains(simplified) || simplified.contains(label)
        }
        if (fuzzy.size == 1) return fuzzy[0].first.go()

        // Multiple matches — list them
        if (contains.size > 1) {
            val names = contains.take(5).joinToString(", ") { it.second }
            return ToolResult(false, "Multiple matches: $names. Please be more specific.")
        }

        return ToolResult(false, "App '$query' not found on this device")
    }

    private fun getLaunchableApps(pm: PackageManager): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return pm.queryIntentActivities(intent, 0)
    }

    private fun launchApp(context: Context, pm: PackageManager, app: ResolveInfo): ToolResult {
        val packageName = app.activityInfo.packageName
        val label = app.loadLabel(pm).toString()
        return try {
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: return ToolResult(false, "Couldn't open $label")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ToolResult(true, "Opening $label")
        } catch (e: Exception) {
            ToolResult(false, "Error: ${e.message}")
        }
    }
}

