import 'package:chainless_frontend/temporary_function_page.dart';
import 'package:chainless_frontend/create_function_page.dart';
import 'package:chainless_frontend/view_function_page.dart';
import 'package:chainless_frontend/widgets/app_bar_buttons.dart';
import 'package:chainless_frontend/widgets/function_view.dart';
import 'package:chainless_frontend/models/models.dart';
import 'package:chainless_frontend/ui_utils.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:flutter/material.dart';
import 'package:flutter_expandable_fab/flutter_expandable_fab.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:provider/provider.dart';

import 'http/api_client.dart';

class ListFunctionsPage extends StatefulWidget {
  const ListFunctionsPage({super.key});

  @override
  State<ListFunctionsPage> createState() => _ListFunctionsPageState();

  static const columnTextStyle =
      TextStyle(fontWeight: FontWeight.bold, fontSize: 24);

  static const columns = [
    DataColumn(label: Text("Name", style: columnTextStyle)),
    DataColumn(label: Text("Status", style: columnTextStyle)),
  ];
}

class _ListFunctionsPageState extends State<ListFunctionsPage> {
  @override
  Widget build(BuildContext context) {
    final apiClient = context.watch<PublicApiClient>();
    return ChainlessScaffold(
      appBar: ChainlessAppBar(
        title: const Text("Functions"),
        actions: [
          IconButton(
            onPressed: () => setState(() {}),
            icon: const Icon(Icons.refresh),
            tooltip: "Refresh",
          ),
          const DocsButton(),
          const DiscordInviteButton(),
        ],
      ),
      body: FutureBuilder(
        future: listFunctions(apiClient),
        builder: (context, snapshot) => snapshot.hasData
            ? loaded(context, apiClient, snapshot.data!)
            : loading,
      ).pad32,
      floatingActionButtonLocation: ExpandableFab.location,
      floatingActionButton: fab(context),
    );
  }

  Widget fab(BuildContext context) => ExpandableFab(
        initialOpen: true,
        children: [
          FloatingActionButton.small(
            heroTag: null,
            tooltip: "Create Temporary Function",
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => const TemporaryFunctionPage(),
              ),
            ),
            child: const Icon(Icons.hourglass_bottom),
          ),
          FloatingActionButton.small(
            heroTag: null,
            tooltip: "Create Persistent Function",
            onPressed: () => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (context) => const CreateFunctionPage(),
              ),
            ),
            child: const Icon(Icons.save),
          ),
        ],
      );

  Future<List<(String, FunctionInfo)>> listFunctions(
          PublicApiClient apiClient) =>
      apiClient
          .listFunctionIds()
          .asyncMap((id) => apiClient.getFunction(id).then((f) => (id, f)))
          .toList();

  Widget get loading => const Center(child: ChainlessLoadingIndicator());

  Widget loaded(BuildContext context, PublicApiClient apiClient,
      List<(String, FunctionInfo)> functions) {
    late Widget child;
    if (functions.isEmpty) {
      child = const NoFunctionsView();
    } else {
      child = FunctionsViewSmallScreen(
          functions: functions,
          deleteFunction: (id) => _deleteFunction(apiClient, id));
    }
    return Center(child: child);
  }

  _deleteFunction(PublicApiClient apiClient, String functionId) async {
    await apiClient.deleteFunction(functionId);
    setState(() {});
  }
}

class NoFunctionsView extends StatelessWidget {
  const NoFunctionsView({super.key});

  @override
  Widget build(BuildContext context) => ChainlessCard(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              "Create a new function to get started.",
              style: TextStyle(fontSize: 28),
            ).pad16,
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                TextButton.icon(
                        onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) =>
                                    const TemporaryFunctionPage(),
                              ),
                            ),
                        icon: const Icon(Icons.hourglass_bottom),
                        label: const Text("Create Temporary Function"))
                    .pad16,
                TextButton.icon(
                        onPressed: () => Navigator.push(
                              context,
                              MaterialPageRoute(
                                builder: (context) =>
                                    const CreateFunctionPage(),
                              ),
                            ),
                        icon: const Icon(Icons.save),
                        label: const Text("Create Persistent Function"))
                    .pad16,
              ],
            )
          ],
        ).pad16,
      );
}

class FunctionsViewSmallScreen extends StatelessWidget {
  final List<(String, FunctionInfo)> functions;
  final Function(String) deleteFunction;

  const FunctionsViewSmallScreen(
      {super.key, required this.functions, required this.deleteFunction});

  @override
  Widget build(BuildContext context) => ConstrainedBox(
        constraints: const BoxConstraints(maxHeight: 600, maxWidth: 500),
        child: ChainlessCard(
          child: Column(
            children: [
              const Padding(
                padding: EdgeInsets.only(top: 16, bottom: 8),
                child: Text(
                  "Persistent Functions",
                  style: TextStyle(fontSize: 24),
                  textAlign: TextAlign.start,
                ),
              ),
              const Divider(),
              Expanded(child: listView(context)),
            ],
          ),
        ),
      );

  Widget listView(BuildContext context) => ListView.separated(
      itemBuilder: (context, index) => listItem(context, index),
      separatorBuilder: (_, __) => const Divider(),
      itemCount: functions.length);

  Widget listItem(BuildContext context, int index) {
    final (functionId, function) = functions[index];

    late Function() onTap;
    late Widget trailingIcon;
    if (function.error != null) {
      onTap = () => showDialog(
            context: context,
            builder: (context) => FunctionErrorDialog(function: function),
            barrierColor: mainColor,
          );
      trailingIcon = const Icon(Icons.error);
    } else {
      onTap = () => Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => FunctionPage(
                functionId: functionId,
                function: function,
                deleteFunction: () => deleteFunction(functionId),
              ),
            ),
          );
      trailingIcon = const Icon(Icons.forward);
    }
    return ListTile(
      leading: languageIcon(function.language),
      title: Text(function.name),
      subtitle: Row(
          children: function.chains
              .map(chainIcon)
              .map((i) => FaIcon(i).pad4)
              .toList()),
      trailing: trailingIcon,
      onTap: onTap,
    );
  }
}

class FunctionErrorDialog extends StatelessWidget {
  final FunctionInfo function;

  const FunctionErrorDialog({super.key, required this.function});

  @override
  Widget build(BuildContext context) {
    return ChainlessCard(
        child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Text("An error occurred when processing your function.",
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
        const Divider(),
        SingleChildScrollView(
            child: Center(child: Text(function.error ?? "", maxLines: 50))),
        TextButton.icon(
          icon: const Icon(Icons.close),
          label: const Text("Close"),
          onPressed: () => Navigator.pop(context),
        ),
      ],
    ).pad32);
  }
}
