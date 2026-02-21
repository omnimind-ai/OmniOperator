import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:flutter_module/services/history_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  test('saveMessageToHistory should ignore blank messages', () async {
    await saveMessageToHistory('   ');
    final history = await getHistoryMessages();
    expect(history, isEmpty);
  });

  test('saveMessageToHistory should deduplicate, keep order and cap at 3', () async {
    await saveMessageToHistory('A');
    await saveMessageToHistory('B');
    await saveMessageToHistory('A');
    await saveMessageToHistory('C');
    await saveMessageToHistory('D');

    final history = await getHistoryMessages();
    expect(history, ['D', 'C', 'A']);
  });
}

