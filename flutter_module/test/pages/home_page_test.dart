import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:flutter_module/l10n/app_strings.dart';
import 'package:flutter_module/pages/home_page.dart';
import 'package:flutter_module/services/app_language_controller.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const methodChannel = MethodChannel('cn.com.omnimind.omnibot/dev_server');
  const eventChannel = MethodChannel('cn.com.omnimind.omnibot/bot_message_events');

  int sendTextCalls = 0;

  Future<void> pumpHomePage(WidgetTester tester) async {
    final languageController = AppLanguageController(AppLanguage.zh);
    await tester.pumpWidget(
      AppStringsScope(
        strings: const AppStrings(AppLanguage.zh),
        child: MaterialApp(
          home: HomePage(languageController: languageController),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  Future<void> disposeHomePage(WidgetTester tester) async {
    await tester.pumpWidget(const SizedBox.shrink());
    await tester.pump(const Duration(seconds: 1));
  }

  setUp(() {
    sendTextCalls = 0;
    SharedPreferences.setMockInitialValues({
      'server_ip_address': '127.0.0.1:5000',
    });
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, (call) async {
          switch (call.method) {
            case 'isAccessibilityServiceEnabled':
            case 'isIgnoringBatteryOptimizations':
              return true;
            case 'sendServerAddress':
              return call.arguments;
            case 'startServer':
              return '127.0.0.1:8080';
            case 'sendTextToNative':
              sendTextCalls += 1;
              return 'SUCCESS';
            case 'setSocketAuthToken':
            case 'setDevServerApiKey':
            case 'setScreenshotQuality':
            case 'setScreenshotResize':
            case 'toggleCompanionMode':
            case 'toggleFinishedBrowsing':
            case 'openAccessibilitySettings':
            case 'openBatteryOptimizationSettings':
            case 'stopServer':
              return null;
            default:
              return null;
          }
        });

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventChannel, (call) async {
          return null;
        });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(methodChannel, null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(eventChannel, null);
  });

  testWidgets('empty input should not send message to native', (tester) async {
    await pumpHomePage(tester);

    await tester.tap(find.byIcon(Icons.arrow_upward_rounded).first);
    await tester.pump();

    expect(sendTextCalls, 0);
    await disposeHomePage(tester);
  });

  testWidgets('dev server mode should disable sending from mobile', (tester) async {
    const strings = AppStrings(AppLanguage.zh);
    await pumpHomePage(tester);

    await tester.tap(find.byIcon(Icons.rocket_launch_outlined).first);
    await tester.pumpAndSettle();

    expect(find.byIcon(Icons.lock_outline), findsOneWidget);
    expect(find.byTooltip(strings.devServerDesktopOnly), findsOneWidget);
    expect(sendTextCalls, 0);
    await disposeHomePage(tester);
  });
}
