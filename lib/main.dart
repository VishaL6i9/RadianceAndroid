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
  
  @override
  void initState() {
    super.initState();
    _checkPermission();
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
      setState(() {
        _currentBrightness = brightness;
        _sliderValue = brightness.toDouble();
        _statusMessage = 'Current brightness: $brightness/255';
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
                      mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                      children: [
                        ElevatedButton(
                          onPressed: _hasPermission ? () => _setBrightness(0) : null,
                          child: const Text('Min (0)'),
                        ),
                        ElevatedButton(
                          onPressed: _hasPermission ? () => _setBrightness(128) : null,
                          child: const Text('Mid (128)'),
                        ),
                        ElevatedButton(
                          onPressed: _hasPermission ? _setMaxBrightness : null,
                          child: const Text('Max (255)'),
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
