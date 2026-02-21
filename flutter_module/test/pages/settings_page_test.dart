import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:flutter_module/l10n/app_strings.dart';
import 'package:flutter_module/pages/settings_page.dart';
import 'package:flutter_module/services/app_language_controller.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const methodChannel = MethodChannel('cn.com.omnimind.omnibot/dev_server');

  late AppLanguageController languageController;
  late List<MethodCall> methodCalls;

  Future<void> pumpSettingsPage(WidgetTester tester) async {
    tester.view.physicalSize = const Size(1200, 2400);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() {
      tester.view.resetPhysicalSize();
      tester.view.resetDevicePixelRatio();
    });

    await tester.pumpWidget(
      AppStringsScope(
        strings: const AppStrings(AppLanguage.zh),
        child: MaterialApp(
          home: SettingsPage(
            isCompanionModeEnabled: false,
            onCompanionModeChanged: (_) {},
            languageController: languageController,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> openAdvancedSection(WidgetTester tester) async {
    await tester.tap(find.byType(ExpansionTile));
    await tester.pumpAndSettle();
  }

  setUp(() {
    languageController = AppLanguageController(AppLanguage.zh);
    methodCalls = [];
    SharedPreferences.setMockInitialValues({
      'server_ip_address': '127.0.0.1:5000',
      'screenshot_jpeg_quality': 50,
      'screenshot_resize_enabled': false,
      'screenshot_scale_percent': 100,
    });

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, (call) async {
          methodCalls.add(call);
          switch (call.method) {
            case 'sendServerAddress':
              return call.arguments;
            case 'setScreenshotQuality':
            case 'setScreenshotResize':
            case 'setSocketAuthToken':
            case 'setDevServerApiKey':
              return null;
            default:
              return null;
          }
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, null);
  });

  testWidgets('screenshot quality should clamp to 1 and trigger save', (
    tester,
  ) async {
    const strings = AppStrings(AppLanguage.zh);
    await pumpSettingsPage(tester);
    await openAdvancedSection(tester);

    await tester.enterText(find.widgetWithText(TextFormField, strings.screenshotQualityInputLabel), '0');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getInt('screenshot_jpeg_quality'), 1);

    final qualityCalls = methodCalls.where((c) => c.method == 'setScreenshotQuality').toList();
    expect(qualityCalls.isNotEmpty, isTrue);
    expect((qualityCalls.last.arguments as Map)['quality'], 1);
  });

  testWidgets('resize switch and scale input should save clamped value and invoke channel', (
    tester,
  ) async {
    const strings = AppStrings(AppLanguage.zh);
    await pumpSettingsPage(tester);
    await openAdvancedSection(tester);

    final resizeSwitchTile = find.widgetWithText(
      SwitchListTile,
      strings.screenshotResizeTitle,
    );
    final tile = tester.widget<SwitchListTile>(resizeSwitchTile);
    tile.onChanged?.call(true);
    await tester.pumpAndSettle();

    final scaleField = find.widgetWithText(
      TextFormField,
      strings.screenshotResizeInputLabel,
    );
    await tester.enterText(scaleField, '101');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();

    final prefs = await SharedPreferences.getInstance();
    expect(prefs.getBool('screenshot_resize_enabled'), isTrue);
    expect(prefs.getInt('screenshot_scale_percent'), 100);

    final resizeCalls = methodCalls.where((c) => c.method == 'setScreenshotResize').toList();
    expect(resizeCalls.isNotEmpty, isTrue);
    expect((resizeCalls.last.arguments as Map)['enabled'], isTrue);
    expect((resizeCalls.last.arguments as Map)['scalePercent'], 100);
  });
}
