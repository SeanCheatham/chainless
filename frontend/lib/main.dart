import 'package:chainless_frontend/http/api_client.dart';
import 'package:chainless_frontend/list_functions_page.dart';
import 'package:chainless_frontend/ui_utils.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:provider/provider.dart';

void main() {
  runApp(const ChainlessApp());
}

class ChainlessApp extends StatefulWidget {
  const ChainlessApp({super.key});

  @override
  State<ChainlessApp> createState() => _ChainlessAppState();
}

class _ChainlessAppState extends State<ChainlessApp> {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Chainless App',
      theme: themeData,
      localizationsDelegates: const <LocalizationsDelegate<dynamic>>[
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('en'),
      ],
      home: const SelectionArea(child: HomePage()),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<StatefulWidget> createState() => HomePageState();
}

class HomePageState extends State<HomePage> {
  @override
  Widget build(BuildContext context) => const AuthorizedPage();
}

class AuthorizedPage extends StatelessWidget {
  const AuthorizedPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Provider.value(
      value: PublicApiClient(),
      child: Navigator(
          key: authenticatedNavigatorKey, onGenerateRoute: onGenerateRoute),
    );
  }

  Route? onGenerateRoute(RouteSettings settings) {
    return MaterialPageRoute(
      builder: (context) => const ListFunctionsPage(),
      settings: settings,
    );
  }
}
