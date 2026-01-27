import 'package:flutter/material.dart';

import 'l10n/app_strings.dart';
import 'pages/home_page.dart';
import 'services/app_language_controller.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final languageController = await AppLanguageController.load();
  runApp(DevServerApp(languageController: languageController));
}

class DevServerApp extends StatelessWidget {
  final AppLanguageController languageController;

  const DevServerApp({super.key, required this.languageController});

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: languageController,
      builder: (context, _) {
        final strings = AppStrings(languageController.language);
        return AppStringsScope(
          strings: strings,
          child: MaterialApp(
            title: strings.appTitle,
            debugShowCheckedModeBanner: false,
            locale: Locale(languageController.language.code),
            theme: ThemeData(
              brightness: Brightness.light,
              scaffoldBackgroundColor: const Color(0xFFF9FAFC),
              primaryColor: const Color(0xFF3982FF),
              fontFamily: 'Roboto',
              colorScheme: const ColorScheme.light(
                primary: Color(0xFF3982FF),
                onPrimary: Colors.white,
                secondary: Color(0xFF4CAF50),
                surface: Colors.white,
                onSurface: Color(0xFF1F2937),
                surfaceVariant: Color(0xFFE5E7EB),
              ),
              useMaterial3: true,
            ),
            home: HomePage(languageController: languageController),
          ),
        );
      },
    );
  }
}
