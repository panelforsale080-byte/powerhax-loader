package astral.loader

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private companion object {
        const val SERVER_LIST_URL = "https://potpot.x10.mx/Loader/index.php"
        const val PANEL_URL = "https://potpot.x10.mx/connect"
        const val DEFAULT_GAME_TYPE = "MLBB"
        const val DEFAULT_GAME_PACKAGE = "com.mobile.legends" // MLBB; was CODM from panel
        const val LIBRARY_FILE_NAME = "libTool.so"
        private const val TAG = "AstralMainActivity"
        const val LIBRARY_DOWNLOAD_URL =
            "https://raw.githubusercontent.com/panelforsale080-byte/lib.so-loader/main/libTool.so"
    }

    init {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    private lateinit var licenseKeyInput: TextInputEditText
    private lateinit var licenseKeyInputLayout: TextInputLayout
    private lateinit var loginButton: Button
    private lateinit var librarySpinner: Spinner
    private lateinit var delaySpinner: Spinner
    private lateinit var methodSpinner: Spinner
    private lateinit var loaderPreferences: LoaderPreferences
    private lateinit var progressBar: ProgressBar
    private lateinit var radioGroup: RadioGroup
    private lateinit var radioAuto: RadioButton
    private lateinit var radioManual: RadioButton
    private lateinit var rootStatusView: TextView
    private lateinit var appBanner: ImageView
    private lateinit var mainScroll: View
    private lateinit var libraryDrawer: View
    private lateinit var drawerScrim: View
    private lateinit var librarySearch: EditText
    private lateinit var libraryList: LinearLayout
    private lateinit var libraryCount: TextView
    
    private val commandRunner = CommandRunner()
    private val catalogClient = LibraryCatalogClient(SERVER_LIST_URL)
    private val assetExtractor by lazy { AssetExtractor(assets, cacheDir) }
    private var isReadyToInject: Boolean = false
    private var isLoadingLibrary: Boolean = false
    private var remoteLibrarySize: Int = 0
    private var selectedLibrary = SelectedLibrary()
    private var delaySeconds: Int = 0
    private var injectionMethod: String = "1"
    private var showTestLibraries = false
    private val allLibraries = mutableListOf<JSONObject>()
    private val filteredLibraries = mutableListOf<JSONObject>()
    private var isDrawerOpen = false
        
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setContentView(R.layout.activity_main)

        mainScroll = findViewById(R.id.main_scroll)
        libraryDrawer = findViewById(R.id.library_drawer)
        drawerScrim = findViewById(R.id.drawer_scrim)
        librarySearch = findViewById(R.id.library_search)
        libraryList = findViewById(R.id.library_list)
        libraryCount = findViewById(R.id.library_count)
        setupLibraryDrawer()
        
        val appCacheDir = cacheDir.absolutePath
        val outputFile = File(appCacheDir, LIBRARY_FILE_NAME)
        if (outputFile.exists()) {
            outputFile.delete()
        }
        
        val versionCode = getAppVersionCode()
        
        progressBar = findViewById(R.id.progressBar)
        
        librarySpinner = findViewById(R.id.my_spinner)
        delaySpinner = findViewById(R.id.delay_spinner)
        methodSpinner = findViewById(R.id.method_spinner)
        licenseKeyInput = findViewById(R.id.input_key)
        licenseKeyInputLayout = findViewById(R.id.input_layout)
        findViewById<View>(R.id.license_key_label).visibility = View.GONE
        licenseKeyInputLayout.visibility = View.GONE
        loginButton = findViewById(R.id.login_button)
        radioGroup = findViewById(R.id.radioGroup)
        radioAuto = findViewById(R.id.radio_auto)
        radioManual = findViewById(R.id.radio_manual)
        rootStatusView = findViewById(R.id.root_status)
        appBanner = findViewById(R.id.app_banner)
        
        Glide.with(this)
            .load(R.drawable.astral_logo)
            .centerCrop()
            .into(appBanner)
        
        loaderPreferences = LoaderPreferences(this)

        val savedMode = loaderPreferences.getMode()
        if (savedMode == "auto") {
            radioAuto.isChecked = true
        } else {
            radioManual.isChecked = true
        }

        setupDelaySpinner()
        setupMethodSpinner()
        
        val savedDelay = loaderPreferences.getDelaySeconds()
        val savedMethod = loaderPreferences.getInjectionMethod()
        
        delaySpinner.setSelection(savedDelay)
        methodSpinner.setSelection(when (savedMethod) {
            "1" -> 0
            "2" -> 1
            "3" -> 2
            else -> 0
        })
        
        delaySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                delaySeconds = position
                loaderPreferences.saveDelaySeconds(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        methodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                injectionMethod = when (position) {
                    0 -> "1"
                    1 -> "2"
                    2 -> "3"
                    else -> "1"
                }
                loaderPreferences.saveInjectionMethod(injectionMethod)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.radio_auto) "auto" else "manual"
            loaderPreferences.saveMode(mode)
        }

        loginButton.setOnClickListener {
            if (isReadyToInject) {
                injectLibrary(LIBRARY_FILE_NAME)
                showToast("Injecting, please wait...")
                loginButton.isEnabled = false
                radioGroup.isEnabled = false
                delaySpinner.isEnabled = false
                methodSpinner.isEnabled = false
            } else if (!isLoadingLibrary) {
                beginLibraryLoad()
            }
        }
        
        appBanner.setOnLongClickListener {
            appBanner.alpha = 0.7f
            Handler(Looper.getMainLooper()).postDelayed({
                appBanner.alpha = 1.0f
            }, 500)
            
            filterLibraries(!showTestLibraries)
            
            val message = if (showTestLibraries) {
                "Test libraries shown"
            } else {
                "Test libraries hidden"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            
            true
        }
        
        loadLibraryCatalog()
        updateRootStatus()
    }
    
    private fun setupDelaySpinner() {
        val delayOptions = arrayOf("NO DELAY", "1S DELAY", "2S DELAY", "3S DELAY", "4S DELAY", "5S DELAY")
        delaySpinner.adapter = SimpleSpinnerAdapter(this, delayOptions)
    }
    
    private fun setupMethodSpinner() {
        val methodOptions = arrayOf("METHOD 1", "METHOD 2", "METHOD 3")
        methodSpinner.adapter = SimpleSpinnerAdapter(this, methodOptions)
    }
    
    private fun applyLibrarySelection(position: Int) {
        if (position !in filteredLibraries.indices) return

        val selectedLibraryJson = filteredLibraries[position]
        handleSelectedJsonData(selectedLibraryJson)
    }
    
    private fun loadLibraryCatalog() {
        lifecycleScope.launch {
            val root = catalogClient.load().getOrElse { _ ->
                Toast.makeText(this@MainActivity, "Failed to load servers data", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val meta = root.optJSONObject("META")
            if (meta != null) {
                val requiredCode = meta.optInt("APP_VERSION_CODE", -1)
                val reason = meta.optString("VERSION_REASON", "Update required.")
                val updateUrl = meta.optString("UPDATE_URL")

                val currentCode = getAppVersionCode()
                if (requiredCode != -1 && currentCode < requiredCode) {
                    showVersionMismatchDialog(reason, updateUrl)
                    return@launch
                }
            }

            runCatching {
                val dataArray = root.getJSONArray("DATA")
                allLibraries.clear()
                for (i in 0 until dataArray.length()) {
                    allLibraries.add(dataArray.getJSONObject(i))
                }
            }.onFailure { _ ->
                Toast.makeText(this@MainActivity, "Failed to load servers data", Toast.LENGTH_SHORT).show()
                return@launch
            }

            refreshLibraryDrawer()
            filterLibraries(false)
        }
    }
    
    private fun setupLibraryDrawer() {
        libraryDrawer.post {
            libraryDrawer.translationX = -libraryDrawer.width.toFloat()
        }

        findViewById<View>(R.id.library_menu_button).setOnClickListener {
            setLibraryDrawerOpen(!isDrawerOpen)
        }
        findViewById<View>(R.id.drawer_close).setOnClickListener {
            setLibraryDrawerOpen(false)
        }
        drawerScrim.setOnClickListener {
            setLibraryDrawerOpen(false)
        }

        librarySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(text: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                refreshLibraryDrawer(text?.toString().orEmpty())
            }
            override fun afterTextChanged(editable: Editable?) = Unit
        })

        findViewById<View>(R.id.drawer_about).setOnClickListener {
            setLibraryDrawerOpen(false)
            AboutDialog(this).show()
        }

        refreshLibraryDrawer()
    }

    private fun setLibraryDrawerOpen(open: Boolean) {
        if (isDrawerOpen == open) return
        isDrawerOpen = open

        if (open) {
            drawerScrim.visibility = View.VISIBLE
            drawerScrim.alpha = 0f
            drawerScrim.animate().alpha(1f).setDuration(180L).start()

            libraryDrawer.visibility = View.VISIBLE
            libraryDrawer.animate().cancel()
            libraryDrawer.animate()
                .translationX(0f)
                .setDuration(260L)
                .setInterpolator(DecelerateInterpolator())
                .start()
            setMainContentBlur(true)
        } else {
            libraryDrawer.animate().cancel()
            libraryDrawer.animate()
                .translationX(-libraryDrawer.width.toFloat())
                .setDuration(220L)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction { libraryDrawer.visibility = View.GONE }
                .start()

            drawerScrim.animate()
                .alpha(0f)
                .setDuration(180L)
                .withEndAction { drawerScrim.visibility = View.GONE }
                .start()
            setMainContentBlur(false)
        }
    }

    @Suppress("NewApi")
    private fun setMainContentBlur(enabled: Boolean) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        mainScroll.setRenderEffect(
            if (enabled) {
                RenderEffect.createBlurEffect(6f, 6f, Shader.TileMode.CLAMP)
            } else {
                null
            }
        )
    }

    private fun refreshLibraryDrawer(query: String = librarySearch.text?.toString().orEmpty()) {
        if (!::libraryList.isInitialized) return

        val normalizedQuery = query.trim()
        val visibleLibraries = allLibraries.filter { library ->
            normalizedQuery.isBlank() ||
                library.optString("NAME").contains(normalizedQuery, ignoreCase = true) ||
                library.optString("GAME").contains(normalizedQuery, ignoreCase = true) ||
                library.optString("TYPE").contains(normalizedQuery, ignoreCase = true)
        }

        libraryList.removeAllViews()
        libraryCount.text = if (allLibraries.isEmpty()) {
            "LOADING LIBRARIES..."
        } else {
            "${visibleLibraries.size} AVAILABLE LIBRARIES"
        }

        if (visibleLibraries.isEmpty()) {
            val emptyState = TextView(this).apply {
                text = if (allLibraries.isEmpty()) "Loading libraries..." else "No matching library"
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.loader_text_muted))
                textSize = 11f
                setPadding(12, 18, 12, 18)
            }
            libraryList.addView(emptyState)
            return
        }

        visibleLibraries.forEach { library ->
            val row = layoutInflater.inflate(R.layout.item_library_drawer, libraryList, false)
            val name = library.optString("NAME").trim().ifBlank { "Unnamed library" }
            val packageName = library.optString("GAME").trim()
            val version = LibraryMetadata.versionLabel(library)
            val gameLabel = LibraryMetadata.gameLabel(library)
            val meta = listOf(gameLabel, version).filter { it.isNotBlank() }.joinToString(" • ")
            val status = LibraryMetadata.status(library)
            val available = status == "ACTIVE"

            row.findViewById<TextView>(R.id.drawer_name).text = name
            row.findViewById<TextView>(R.id.drawer_meta).text = meta
            row.findViewById<TextView>(R.id.drawer_package).apply {
                text = packageName
                visibility = if (packageName.isBlank()) View.GONE else View.VISIBLE
            }
            row.findViewById<TextView>(R.id.drawer_status).apply {
                text = status
                setTextColor(
                    ContextCompat.getColor(
                        this@MainActivity,
                        if (available) R.color.loader_status_active else R.color.loader_status_maintenance
                    )
                )
                background = ContextCompat.getDrawable(
                    this@MainActivity,
                    if (available) R.drawable.status_pill else R.drawable.maintenance_pill
                )
            }
            row.background = ContextCompat.getDrawable(
                this,
                if (available && name == selectedLibrary.name) {
                    R.drawable.drawer_item_selected
                } else {
                    R.drawable.drawer_item
                }
            )
            row.isEnabled = available
            row.alpha = if (available) 1f else 0.58f

            Glide.with(this)
                .load(library.optString("LOGO"))
                .placeholder(R.drawable.astral_logo)
                .error(R.drawable.astral_logo)
                .centerCrop()
                .into(row.findViewById(R.id.drawer_logo))

            row.setOnClickListener {
                if (available) {
                    selectLibraryFromDrawer(library)
                } else {
                    showToast("$name is under maintenance")
                }
            }
            libraryList.addView(row)
        }
    }

    private fun selectLibraryFromDrawer(library: JSONObject) {
        if (!LibraryMetadata.isActive(library)) {
            showToast("${library.optString("NAME", "Library")} is under maintenance")
            return
        }
        handleSelectedJsonData(library)
        val filteredPosition = filteredLibraries.indexOfFirst {
            it.optString("NAME") == library.optString("NAME")
        }
        if (filteredPosition >= 0) {
            librarySpinner.setSelection(filteredPosition, false)
        }
        saveSelectedLibrary(selectedLibrary.name)
        setLibraryDrawerOpen(false)
    }

    private fun filterLibraries(includeTestLibraries: Boolean) {
        showTestLibraries = includeTestLibraries
        filteredLibraries.clear()
        if (includeTestLibraries) {
            filteredLibraries.addAll(allLibraries.filter { library ->
                library.optString("NAME").contains("TEST", ignoreCase = true) &&
                    LibraryMetadata.isActive(library)
            })
        } else {
            filteredLibraries.addAll(allLibraries.filter { library ->
                !library.optString("NAME").contains("TEST", ignoreCase = true) &&
                    LibraryMetadata.isActive(library)
            })
        }
        updateSpinnerWithFilteredData()
    }
    
    private fun updateSpinnerWithFilteredData() {
        val nameList = mutableListOf<String>()
        val logoList = mutableListOf<String>()
        
        for (item in filteredLibraries) {
            val name = item.optString("NAME")
            val logo = item.optString("LOGO")
            nameList.add(name)
            logoList.add(logo)
        }
        
        val adapter = object : ArrayAdapter<String>(this, R.layout.spinner_item, nameList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                var view = convertView
                if (view == null) {
                    val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                    view = inflater.inflate(R.layout.spinner_item, parent, false)
                }
                val nameTextView = view!!.findViewById<TextView>(R.id.spinnerText)
                val logoImageView = view.findViewById<ImageView>(R.id.logoImage)
                nameTextView.text = " ${nameList[position]}"
                val logoUrl = logoList[position]
                Glide.with(this@MainActivity)
                    .load(logoUrl)
                    .into(logoImageView)

                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return getView(position, convertView, parent)
            }
        }
        
        librarySpinner.adapter = adapter

        librarySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                applyLibrarySelection(position)
            }
            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }
        
        val savedLibraryName = loaderPreferences.licenses.getSelectedLibrary()
        val savedPosition = filteredLibraries.indexOfFirst {
            it.optString("NAME") == savedLibraryName
        }
        val initialPosition = if (savedPosition >= 0) savedPosition else 0

        if (filteredLibraries.isNotEmpty()) {
            librarySpinner.setSelection(initialPosition, false)
            val initialName = filteredLibraries[initialPosition].optString("NAME")
            if (selectedLibrary.name != initialName || selectedLibrary.connectUrl.isBlank()) {
            applyLibrarySelection(initialPosition)
            }
        }
    }
    
    fun handleSelectedJsonData(jsonObject: JSONObject) {
        selectedLibrary = SelectedLibrary.fromJson(jsonObject)
        val maintenance = !LibraryMetadata.isActive(jsonObject)
        findViewById<TextView>(R.id.library_name).text = selectedLibrary.name
        findViewById<TextView>(R.id.library_game).text = LibraryMetadata.gameLabel(jsonObject)
        findViewById<TextView>(R.id.library_version).apply {
            text = LibraryMetadata.versionLabel(jsonObject)
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.library_package).apply {
            text = selectedLibrary.gamePackage
            visibility = if (selectedLibrary.gamePackage.isBlank()) View.GONE else View.VISIBLE
        }
        findViewById<TextView>(R.id.library_status).apply {
            text = if (maintenance) "● Maintenance" else "● Ready"
            setTextColor(
                ContextCompat.getColor(
                    this@MainActivity,
                    if (maintenance) R.color.loader_status_maintenance else R.color.loader_status_active
                )
            )
            background = ContextCompat.getDrawable(
                this@MainActivity,
                if (maintenance) R.drawable.maintenance_pill else R.drawable.status_pill
            )
        }
        if (maintenance) {
            isReadyToInject = false
            isLoadingLibrary = false
            loginButton.text = "UNAVAILABLE"
            loginButton.isEnabled = false
        } else {
            isReadyToInject = false
            loginButton.isEnabled = true
            loginButton.text = if (isLoadingLibrary) "LOADING..." else "LOAD LIBRARY"
            beginLibraryLoad()
        }
        val logoUrl = jsonObject.optString("LOGO")
        if (logoUrl.isNotBlank()) {
            Glide.with(this)
                .load(logoUrl)
                .placeholder(R.drawable.circle)
                .error(R.drawable.circle)
                .into(appBanner)
        }
        
        refreshLibraryDrawer()
    }
    
    private fun showVersionMismatchDialog(message: String, updateUrl: String?) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
                .setTitle("Update Required")
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton("Exit") { _, _ ->
                    finishAffinity()
                }

            if (!updateUrl.isNullOrEmpty()) {
                builder.setPositiveButton("Update") { _, _ ->
                        try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(updateUrl)
                            )
                        )
                        finishAffinity()
                    } catch (e: Exception) {
                        finishAffinity()
                    }
                }
            }

            builder.show()
        }
    }
 
    private fun getAppVersionCode(): Int {
        return try {
            packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun beginLibraryLoad() {
        if (isLoadingLibrary || isReadyToInject) return
        val detectedVpn = SplashActivity.getDetectedVPN()
        if (detectedVpn != null) {
            SplashActivity.showDialog(
                this,
                "VPN DETECTED",
                "Detected VPN Interface:\n$detectedVpn\n\nPlease disable your VPN and try again."
            )
            return
        }
        isLoadingLibrary = true
        loginButton.isEnabled = false
        librarySpinner.isEnabled = false
        delaySpinner.isEnabled = false
        methodSpinner.isEnabled = false
        radioGroup.isEnabled = false
        saveSelectedLibrary(selectedLibrary.name)
        loginButton.text = "LOADING..."
        proceedWithDownload()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun saveSelectedLibrary(libraryName: String) {
        loaderPreferences.licenses.saveSelectedLibrary(libraryName)
    }

    private fun resetLoginState(message: String) {
        showToast(message)
        isLoadingLibrary = false
        loginButton.text = "RETRY LOAD"
        loginButton.isEnabled = true
        librarySpinner.isEnabled = true
        delaySpinner.isEnabled = true
        methodSpinner.isEnabled = true
        radioGroup.isEnabled = true
        isReadyToInject = false
    }

    private fun proceedWithDownload() {
        val detectedVpn = SplashActivity.getDetectedVPN()
        if (detectedVpn == null) {
            downloadLibraryIfNeeded(this@MainActivity, LIBRARY_DOWNLOAD_URL, LIBRARY_FILE_NAME) { success ->
                if (success) {
                    showToast("Download successful")
                    assetExtractor.extractAll()
                    if (!isLibraryReady()) {
                        resetLoginState("FAILED TO LOAD RUNTIME LIBRARY")
                        isReadyToInject = false
                    } else {
                        lifecycleScope.launch(Dispatchers.Main) {
                            delay(2500)
                            isLoadingLibrary = false
                            loginButton.text = "INJECT"
                            showToast("Ready to inject")
                            loginButton.isEnabled = true
                            radioGroup.isEnabled = true
                            delaySpinner.isEnabled = true
                            methodSpinner.isEnabled = true
                            librarySpinner.isEnabled = true
                            isReadyToInject = true
                        }
                    }
                } else {
                    resetLoginState("FAILED TO DOWNLOAD, \nRETRY AGAIN.")
                    isReadyToInject = false
                }
            }
        } else {
            resetLoginState("VPN detected — disable VPN to download")
        }
    }

    private fun isLibraryReady(): Boolean {
        val libraryFile = File(cacheDir, LIBRARY_FILE_NAME)
        return libraryFile.isFile && libraryFile.length() > 0L
    }
    
    private fun injectLibrary(libraryFileName: String) {
        val appCacheDir = cacheDir.absolutePath
        val outputFile = File(appCacheDir, libraryFileName)
        if (!outputFile.exists() || outputFile.length() <= 0L) {
            return
        }
        if (remoteLibrarySize > 0 && outputFile.length().toInt() != remoteLibrarySize) {
            return
        }
        remoteLibrarySize = outputFile.length().toInt()
        
        val cacheDirectory = File(cacheDir.absolutePath)
        cacheDirectory.setReadable(true, false)
        cacheDirectory.setWritable(true, false)
        cacheDirectory.setExecutable(true, false)
        cacheDirectory.listFiles()?.forEach { file ->
            file.setExecutable(true, false)
        }
        if (!isLibraryReady()) {
            resetLoginState("FAILED TO LOAD RUNTIME LIBRARY")
            return
        }
        val injectLibrary = libraryForInjection()
        val gamePackage = injectLibrary.gamePackage.ifBlank { DEFAULT_GAME_PACKAGE }
        Log.i(TAG, "Inject package=$gamePackage method=$injectionMethod")
        val injectionCommand = InjectionCommandBuilder.build(
            cacheDirectory = cacheDirectory,
            library = injectLibrary,
            libraryFileName = libraryFileName,
            injectionMethod = injectionMethod,
            hasRootAccess = commandRunner.hasRootAccess()
        )
        Log.i(TAG, "Inject command: $injectionCommand")
        if (radioAuto.isChecked) {
            launchGame(gamePackage)
            lifecycleScope.launch {
                delay(1000)
                delay(delaySeconds * 1000L)
                runInjectPipeline(outputFile, gamePackage, injectionCommand)
            }
        } else {
            lifecycleScope.launch {
                runInjectPipeline(outputFile, gamePackage, injectionCommand)
            }
        }
    }

    private suspend fun runInjectPipeline(
        libraryFile: File,
        gamePackage: String,
        injectionCommand: String
    ) {
        val procBefore = withContext(Dispatchers.IO) { ProcessFinder.findMlbb() }
        withContext(Dispatchers.Main) {
            if (procBefore != null) {
                Log.i(TAG, "MLBB process OK pid=${procBefore.pid} cmdline=${procBefore.label}")
                showToast("Process: pid ${procBefore.pid}")
            } else {
                Log.w(TAG, "MLBB process NOT found before inject")
                showToast("Process not found — open MLBB in lobby first")
            }
            if (!commandRunner.hasRootAccess()) {
                showToast("Root recommended (Method 2/3) for MLBB inject")
            }
        }

        val gspace = withContext(Dispatchers.IO) { MlbbInjectStaging.detectGspaceFromProc() }
        val stageScript = MlbbInjectStaging.buildRootCopyScript(gamePackage, libraryFile, gspace)
        if (stageScript.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val stageResult = commandRunner.runSync(stageScript)
                Log.i(TAG, "Stage libTool.so exit=${stageResult.exitCode}")
            }
        }

        val shellResult = withContext(Dispatchers.IO) {
            commandRunner.runSync(injectionCommand)
        }
        Log.i(TAG, "Inject shell exit=${shellResult.exitCode}")

        delay(1500)
        val procAfter = withContext(Dispatchers.IO) { ProcessFinder.findMlbb() }
        val loaded = procAfter?.let { InjectVerifier.isLibLoaded(it.pid) } == true

        withContext(Dispatchers.Main) {
            when {
                loaded -> {
                    showToast("Inject OK — libTool in game memory")
                    loginButton.text = "RE-INJECT"
                }
                shellResult.exitCode != 0 -> {
                    showToast("Inject failed (exit ${shellResult.exitCode}) — check logcat Astral*")
                    loginButton.text = "RETRY INJECT"
                }
                procAfter == null -> {
                    showToast("Inject ran but game process gone — reopen MLBB")
                    loginButton.text = "RE-INJECT"
                }
                else -> {
                    showToast("Inject unclear — no libTool in maps. Try Method 3 + root + delay")
                    loginButton.text = "RE-INJECT"
                }
            }
            loginButton.isEnabled = true
            radioGroup.isEnabled = true
            delaySpinner.isEnabled = true
            methodSpinner.isEnabled = true
        }
    }

    private fun libraryForInjection(): SelectedLibrary {
        val pkg = SelectedLibrary.normalizeGamePackage(selectedLibrary.gamePackage)
        val arch = SelectedLibrary.resolveArchitecture(selectedLibrary.architecture)
        return selectedLibrary.copy(gamePackage = pkg, architecture = arch)
    }

    
    private fun launchGame(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
                true
            } else {
                Toast.makeText(this, "Game not installed!", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to start game!", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    private fun downloadLibraryIfNeeded(
        context: Context,
        url: String,
        libraryFileName: String,
        callback: (Boolean) -> Unit
    ) {
        val libraryDownloader = LibraryDownloader(context)
        val outputFile = File(context.cacheDir, libraryFileName)
        lifecycleScope.launch(Dispatchers.IO) {
            var success = false
            try {
                val connection = URL(url).openConnection()
                try {
                    connection.connect()
                    val reportedSize = connection.contentLength
                    remoteLibrarySize = if (reportedSize > 0) reportedSize else 0
                    val needToDownload = if (outputFile.exists() && outputFile.length() > 0L) {
                        remoteLibrarySize > 0 && outputFile.length() != remoteLibrarySize.toLong()
                    } else {
                        true
                    }

                    if (!needToDownload) {
                        remoteLibrarySize = outputFile.length().toInt()
                        success = true
                    } else {
                        withContext(Dispatchers.Main) {
                            showToast("Downloading libTool.so...")
                            progressBar.visibility = View.VISIBLE
                            progressBar.progress = 0
                        }
                        success = libraryDownloader.download(url, libraryFileName) { progress ->
                            runOnUiThread {
                                progressBar.progress = progress
                            }
                        }
                        if (success && outputFile.exists()) {
                            remoteLibrarySize = outputFile.length().toInt()
                        }
                    }
                } finally {
                    (connection as? HttpURLConnection)?.disconnect()
                }
            } catch (_: Exception) {
            }

            withContext(Dispatchers.Main) {
                callback(success)
                if (::progressBar.isInitialized) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    
    private fun updateRootStatus() {
        lifecycleScope.launch(Dispatchers.IO) {
            val isRooted = commandRunner.hasRootAccess()
            withContext(Dispatchers.Main) {
                if (isRooted) {
                    rootStatusView.text = "ROOTED ENVIRONMENT"
                    rootStatusView.setTextColor(Color.GREEN)
                    rootStatusView.visibility = View.VISIBLE
                } else {
                    rootStatusView.visibility = View.GONE
                }
            }
        }
    }
    
    private fun cleanUp() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        LibraryDownloader.cancelNotification(this)
    }
    
    override fun onStop() {
        super.onStop()
        if (isFinishing) {
            cleanUp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }
}
