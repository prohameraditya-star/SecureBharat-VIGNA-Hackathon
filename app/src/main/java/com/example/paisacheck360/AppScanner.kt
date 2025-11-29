package com.example.paisacheck360

import android.Manifest
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppScanner(private val context: Context) {

    // SharedPreferences for storing last known permissions per app
    private val prefs by lazy {
        context.getSharedPreferences("AppPermissionHistory", Context.MODE_PRIVATE)
    }

    // 🔹 Allowed permissions for each app category
    private val ALLOWED_PERMISSIONS_BY_CATEGORY = mapOf(
        "BANKING" to listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.INTERNET
        ),
        "SOCIAL" to listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.INTERNET
        ),
        "CAMERA" to listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ),
        "MUSIC" to listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ),
        "GAME" to listOf(
            Manifest.permission.INTERNET
        ),
        "GENERAL" to emptyList()
    )

    // 🔹 Optimized scam keyword list for APP PACKAGE NAME detection
    private val scamKeywords = listOf(
        // Fake loan apps
        "loan", "instantloan", "quickloan", "easyloan", "fastloan", "nocibil", "emiloan",
        "homeloan", "educationloan", "goldloan", "approvedloan",
        // Fake wallet & payment rewards
        "freecash", "bonuswallet", "rewardwallet", "instantcash", "cashbonus", "cashreward",
        // Fake payment apps / clones
        "paytmpro", "paytmfree", "phonepeplus", "phonepay", "gpayfree", "bhimfree",
        // Fake bank apps
        "accountupdate", "banksecure", "kycupdate", "onlinebanking", "securebanking",
        // Earn-money scam apps
        "earnmoney", "makemoney", "passiveincome", "workfromhome", "dailyincome",
        // Crypto scam apps
        "cryptotrading", "bitcoinmining", "binancepro", "blockchainwallet", "tradingexpert",
        // Impersonation clones (general)
        "verifiedwallet", "supportteam", "helpdesk", "customersupport"
    )

    // Categorize based on package name patterns
    private fun getAppCategory(appInfo: ApplicationInfo): String {
        val pkg = appInfo.packageName.lowercase()

        return when {
            pkg.contains("bank") || pkg.contains("pay") ||
                    pkg.contains("upi") || pkg.contains("wallet") -> "BANKING"
            pkg.contains("social") || pkg.contains("chat") ||
                    pkg.contains("insta") || pkg.contains("whatsapp") ||
                    pkg.contains("facebook") -> "SOCIAL"
            pkg.contains("cam") || pkg.contains("photo") ||
                    pkg.contains("scanner") -> "CAMERA"
            pkg.contains("game") -> "GAME"
            pkg.contains("music") || pkg.contains("audio") -> "MUSIC"
            else -> "GENERAL"
        }
    }

    // Detect installer source safely
    private fun getInstallerSource(packageName: String): String {
        val installer = context.packageManager.getInstallerPackageName(packageName) ?: ""
        return when {
            installer.contains("com.android.vending") -> "PLAY_STORE"
            installer.contains("com.google.android.packageinstaller") -> "PACKAGE_INSTALLER"
            installer.isBlank() -> "UNKNOWN"
            else -> "THIRD_PARTY"
        }
    }

    // 🔹 Load previously saved permissions for this app
    private fun getSavedPermissions(packageName: String): Set<String> {
        return prefs.getStringSet(packageName, emptySet()) ?: emptySet()
    }

    // 🔹 Save current permissions for next comparison
    private fun savePermissions(packageName: String, permissions: List<String>) {
        prefs.edit().putStringSet(packageName, permissions.toSet()).apply()
    }

    /**
     * 🔍 Full app scan with advanced risk scoring (Phase 1 + 2 + 3)
     */
    suspend fun scanAllApps(): List<AppRiskInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val result = mutableListOf<AppRiskInfo>()

        installedApps.forEach { appInfo ->
            try {
                val pInfo = pm.getPackageInfo(appInfo.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = pInfo.requestedPermissions?.toList() ?: emptyList()

                // Base permission score
                val (baseScore, dangerousPermissions) =
                    PermissionAnalyzer.analyzePermissions(permissions)

                var score = baseScore

                // ------------------------------------------------
                // Phase-3: Compare with previous permissions (history)
                // ------------------------------------------------
                val previousPermissions = getSavedPermissions(appInfo.packageName)
                val previousSet = previousPermissions.toSet()
                val newlyAddedPermissions = permissions.filter { !previousSet.contains(it) }

                // Newly added dangerous permissions = intersection of new perms & dangerous ones
                val newDangerousPermissions = dangerousPermissions.filter { newlyAddedPermissions.contains(it) }

                // Scoring: penalize risky additions
                if (newDangerousPermissions.isNotEmpty()) {
                    // +10 per new dangerous permission
                    score += 10 * newDangerousPermissions.size

                    // Big jump: many dangerous perms added at once
                    if (newDangerousPermissions.size >= 3) {
                        score += 10
                    }
                }

                // If app removed dangerous permissions, reduce score slightly
                val removedPermissions = previousSet.filter { !permissions.contains(it) }
                if (removedPermissions.isNotEmpty() &&
                    removedPermissions.any { dangerousPermissions.contains(it) }
                ) {
                    score -= 5
                }

                // ------------------------------------------------
                // Phase-1: App Category vs Permission check
                // ------------------------------------------------
                val category = getAppCategory(appInfo)
                val allowed = ALLOWED_PERMISSIONS_BY_CATEGORY[category] ?: emptyList()

                dangerousPermissions.forEach { perm ->
                    if (!allowed.contains(perm)) score += 5
                }

                // ------------------------------------------------
                // Phase-2: Installer & Scam Reputation check
                // ------------------------------------------------
                val installer = getInstallerSource(appInfo.packageName)

                when (installer) {
                    "PLAY_STORE" -> score -= 5    // safety bonus
                    "PACKAGE_INSTALLER" -> score += 8
                    "THIRD_PARTY" -> score += 15   // risky store
                    "UNKNOWN" -> score += 20       // APK sideload
                }

                val pkg = appInfo.packageName.lowercase()
                if (scamKeywords.any { pkg.contains(it) }) {
                    score += 25     // fake / scam-like package naming
                }

                // Boundary
                score = score.coerceIn(0, 100)

                // Risk level conversion
                val level = PermissionAnalyzer.getRiskLevel(score)

                // App details
                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val installDate = pInfo.firstInstallTime
                val updateDate = pInfo.lastUpdateTime

                // Save current permissions for future scan diff
                savePermissions(appInfo.packageName, permissions)

                // Build final object with Phase-3 info
                result.add(
                    AppRiskInfo(
                        appName = appName,
                        packageName = appInfo.packageName,
                        icon = icon,
                        riskLevel = level,
                        riskScore = score,
                        dangerousPermissions = dangerousPermissions,
                        totalPermissions = permissions.size,
                        installDate = installDate,
                        lastUpdateDate = updateDate,
                        isSystemApp = isSystemApp,
                        newDangerousPermissions = newDangerousPermissions
                    )
                )

            } catch (_: Exception) {
                // Skip silently if any app cannot be analyzed
            }
        }

        result.sortedByDescending { it.riskScore }
    }

    /**
     * 📊 Summary for dashboard counts
     */
    suspend fun getStatistics(): AppScanStatistics = withContext(Dispatchers.IO) {
        val apps = scanAllApps()
        AppScanStatistics(
            totalApps = apps.size,
            criticalApps = apps.count { it.riskLevel == RiskLevel.CRITICAL },
            highRiskApps = apps.count { it.riskLevel == RiskLevel.HIGH },
            mediumRiskApps = apps.count { it.riskLevel == RiskLevel.MEDIUM },
            lowRiskApps = apps.count { it.riskLevel == RiskLevel.LOW }
        )
    }
}
