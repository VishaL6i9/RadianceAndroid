import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Force Max Brightness',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepOrange),
        useMaterial3: true,
      ),
      home: const BrightnessControlPage(),
    );
  }
}

class BrightnessControlPage extends StatefulWidget {
  const BrightnessControlPage({super.key});

  @override
  State<BrightnessControlPage> createState() => _BrightnessControlPageState();
}

class _BrightnessControlPageState extends State<BrightnessControlPage> {
  static const platform = MethodChannel('com.example.force_max_brightness/brightness');
  
  bool _hasPermission = false;
  int _currentBrightness = 128;
  double _sliderValue = 128;
  String _statusMessage = 'Initializing...';
  int _brightnessMode = 0; // 0 = MANUAL, 1 = AUTO
  bool _windowBrightnessActive = false;
  bool _serviceRunning = false;
  bool _autoStartEnabled = false;
  
  @override
  void initState() {
    super.initState();
    _checkPermission();
    _loadSettings();
  }
  
  Future<void> _loadSettings() async {
    try {
      final bool autoStart = await platform.invokeMethod('getAutoStart');
      setState(() {
        _autoStartEnabled = autoStart;
      });
    } catch (e) {
      // Ignore errors during settings load
    }
  }
  
  Future<void> _checkPermission() async {
    try {
      final bool result = await platform.invokeMethod('canWriteSettings');
      setState(() {
        _hasPermission = result;
        _statusMessage = result 
            ? 'Permission granted' 
            : 'Permission required';
      });
      if (result) {
        _getCurrentBrightness();
      }
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _requestPermission() async {
    try {
      await platform.invokeMethod('requestWriteSettingsPermission');
      setState(() {
        _statusMessage = 'Please grant permission and return to app';
      });
      // Wait a bit then recheck
      Future.delayed(const Duration(seconds: 2), _checkPermission);
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _getCurrentBrightness() async {
    try {
      final int brightness = await platform.invokeMethod('getSystemBrightness');
      final int mode = await platform.invokeMethod('getBrightnessMode');
      setState(() {
        _currentBrightness = brightness;
        _sliderValue = brightness.toDouble();
        _brightnessMode = mode;
        _statusMessage = 'Brightness: $brightness/255, Mode: ${mode == 0 ? "MANUAL" : "AUTO"}';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error reading brightness: ${e.message}';
      });
    }
  }
  
  Future<void> _setBrightness(int brightness) async {
    try {
      await platform.invokeMethod('setSystemBrightness', {'brightness': brightness});
      setState(() {
        _currentBrightness = brightness;
        _statusMessage = 'Brightness set to $brightness/255';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _setMaxBrightness() async {
    await _setBrightness(255);
    _sliderValue = 255;
  }
  
  Future<void> _setWindowBrightness(int brightness) async {
    try {
      await platform.invokeMethod('setWindowBrightness', {'brightness': brightness});
      setState(() {
        _windowBrightnessActive = brightness != -1;
        _statusMessage = brightness == -1 
            ? 'Window brightness reset to system'
            : 'Window brightness set to $brightness/255';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _setBrightnessMode(int mode) async {
    try {
      await platform.invokeMethod('setBrightnessMode', {'mode': mode});
      setState(() {
        _brightnessMode = mode;
        _statusMessage = 'Brightness mode set to ${mode == 0 ? "MANUAL" : "AUTO"}';
      });
      _getCurrentBrightness();
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _startMediaMonitor() async {
    try {
      await platform.invokeMethod('startMediaMonitor');
      setState(() {
        _serviceRunning = true;
        _statusMessage = 'Media monitor started';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _stopMediaMonitor() async {
    try {
      await platform.invokeMethod('stopMediaMonitor');
      setState(() {
        _serviceRunning = false;
        _statusMessage = 'Media monitor stopped';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _setAutoStart(bool enabled) async {
    try {
      await platform.invokeMethod('setAutoStart', {'enabled': enabled});
      setState(() {
        _autoStartEnabled = enabled;
        _statusMessage = enabled 
            ? 'Auto-start enabled (will start on boot)'
            : 'Auto-start disabled';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error: ${e.message}';
      });
    }
  }
  
  Future<void> _readSetting(String key, bool isGlobal) async {
    try {
      final String? value = await platform.invokeMethod('getSystemSetting', {
        'key': key,
        'isGlobal': isGlobal,
      });
      setState(() {
        _statusMessage = '$key = ${value ?? "null"}';
      });
      _showDialog('Setting: $key', value ?? 'null or not found');
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error reading $key: ${e.message}';
      });
    }
  }
  
  Future<void> _writeSetting(String key, String value, bool isGlobal) async {
    try {
      await platform.invokeMethod('setSystemSetting', {
        'key': key,
        'value': value,
        'isGlobal': isGlobal,
      });
      setState(() {
        _statusMessage = 'Set $key = $value';
      });
    } on PlatformException catch (e) {
      setState(() {
        _statusMessage = 'Error writing $key: ${e.message}';
      });
    }
  }
  
  void _showDialog(String title, String content) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(content),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }
  
  void _showSettingDialog(String title, String key, bool isGlobal) {
    final controller = TextEditingController();
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(
            hintText: 'Enter value',
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              _writeSetting(key, controller.text, isGlobal);
            },
            child: const Text('Set'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: const Text('Force Max Brightness'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Permission Status Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          _hasPermission ? Icons.check_circle : Icons.warning,
                          color: _hasPermission ? Colors.green : Colors.orange,
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          'Permission Status',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(_statusMessage),
                    if (!_hasPermission) ...[
                      const SizedBox(height: 8),
                      ElevatedButton(
                        onPressed: _requestPermission,
                        child: const Text('Grant Permission'),
                      ),
                    ],
                    if (_hasPermission) ...[
                      const SizedBox(height: 8),
                      ElevatedButton(
                        onPressed: _checkPermission,
                        child: const Text('Refresh Status'),
                      ),
                    ],
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Brightness Control Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Brightness Control',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text('Current: ${_currentBrightness}/255 (${(_currentBrightness / 255 * 100).toStringAsFixed(0)}%)'),
                    Text('Mode: ${_brightnessMode == 0 ? "MANUAL" : "AUTO"}', 
                         style: TextStyle(color: _brightnessMode == 1 ? Colors.orange : Colors.green)),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.end,
                      children: [
                        TextButton(
                          onPressed: _hasPermission ? () => _setBrightnessMode(0) : null,
                          child: const Text('Manual'),
                        ),
                        TextButton(
                          onPressed: _hasPermission ? () => _setBrightnessMode(1) : null,
                          child: const Text('Auto'),
                        ),
                      ],
                    ),
                    Slider(
                      value: _sliderValue,
                      min: 0,
                      max: 255,
                      divisions: 255,
                      label: _sliderValue.round().toString(),
                      onChanged: _hasPermission ? (value) {
                        setState(() {
                          _sliderValue = value;
                        });
                      } : null,
                      onChangeEnd: _hasPermission ? (value) {
                        _setBrightness(value.round());
                      } : null,
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _hasPermission ? () => _setBrightness(0) : null,
                            child: const Text('Min'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _hasPermission ? () => _setBrightness(128) : null,
                            child: const Text('Mid'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: _hasPermission ? _setMaxBrightness : null,
                            child: const Text('Max'),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    ElevatedButton.icon(
                      onPressed: _hasPermission ? _getCurrentBrightness : null,
                      icon: const Icon(Icons.refresh),
                      label: const Text('Read Current'),
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Window Brightness Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Text(
                          'Window Brightness Override',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const Spacer(),
                        if (_windowBrightnessActive)
                          const Icon(Icons.flash_on, color: Colors.orange),
                      ],
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'App-level brightness (instant, no permission needed)',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () => _setWindowBrightness(255),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.orange,
                            ),
                            child: const Text('Max'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () => _setWindowBrightness(128),
                            child: const Text('Mid'),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () => _setWindowBrightness(-1),
                            child: const Text('Reset'),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // Media Monitor Service Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Text(
                          'Auto-Brightness Monitor',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const Spacer(),
                        if (_serviceRunning)
                          const Icon(Icons.circle, color: Colors.green, size: 16),
                      ],
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Automatically boost brightness during media playback',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      '⚠️ Requires Notification Access permission (Settings > Special Access)',
                      style: TextStyle(fontSize: 11, color: Colors.orange),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: !_serviceRunning ? _startMediaMonitor : null,
                            icon: const Icon(Icons.play_arrow, size: 20),
                            label: const Text('Start'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.green,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: ElevatedButton.icon(
                            onPressed: _serviceRunning ? _stopMediaMonitor : null,
                            icon: const Icon(Icons.stop, size: 20),
                            label: const Text('Stop'),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.red,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    SwitchListTile(
                      title: const Text('Auto-start on boot'),
                      subtitle: const Text('Start monitoring when device boots'),
                      value: _autoStartEnabled,
                      onChanged: _setAutoStart,
                    ),
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 16),
            
            // HDR Settings Card
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'HDR Settings (Experimental)',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Test reading/writing HDR-related system settings',
                      style: TextStyle(fontSize: 12, color: Colors.grey),
                    ),
                    const SizedBox(height: 16),
                    ListTile(
                      title: const Text('minimumHdrPercentOfScreen'),
                      subtitle: const Text('Android 14+ HDR setting'),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: const Icon(Icons.search),
                            onPressed: _hasPermission 
                                ? () => _readSetting('minimumHdrPercentOfScreen', false)
                                : null,
                          ),
                          IconButton(
                            icon: const Icon(Icons.edit),
                            onPressed: _hasPermission 
                                ? () => _showSettingDialog('Set HDR %', 'minimumHdrPercentOfScreen', false)
                                : null,
                          ),
                        ],
                      ),
                    ),
                    ListTile(
                      title: const Text('peak_refresh_rate'),
                      subtitle: const Text('System setting'),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: const Icon(Icons.search),
                            onPressed: _hasPermission 
                                ? () => _readSetting('peak_refresh_rate', false)
                                : null,
                          ),
                          IconButton(
                            icon: const Icon(Icons.edit),
                            onPressed: _hasPermission 
                                ? () => _showSettingDialog('Set Peak Refresh', 'peak_refresh_rate', false)
                                : null,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
