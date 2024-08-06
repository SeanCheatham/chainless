import 'package:chainless_frontend/http/api_client.dart';
import 'package:chainless_frontend/list_functions_page.dart';
import 'package:chainless_frontend/models/models.dart';
import 'package:chainless_frontend/widgets/app_bar_buttons.dart';
import 'package:chainless_frontend/widgets/function_view.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

class FunctionPage extends StatefulWidget {
  final String functionId;
  final FunctionInfo function;
  final Function() deleteFunction;

  const FunctionPage(
      {super.key,
      required this.functionId,
      required this.function,
      required this.deleteFunction});

  @override
  State<FunctionPage> createState() => _FunctionPageState();
}

class _FunctionPageState extends State<FunctionPage> {
  FunctionInfo? function;

  @override
  void initState() {
    super.initState();
    function = widget.function;
  }

  @override
  Widget build(BuildContext context) {
    final apiClient = context.watch<PublicApiClient>();
    return ChainlessScaffold(
      appBar: ChainlessAppBar(title: const Text("Function Info"), actions: [
        IconButton(
            onPressed: () => refresh(apiClient),
            icon: const Icon(Icons.refresh)),
        deleteButton(context),
        const DocsButton(suffix: "/docs/persistent-functions"),
        const DiscordInviteButton(),
      ]),
      body: function != null
          ? FunctionView(functionId: widget.functionId, function: function!)
          : loading,
    );
  }

  refresh(PublicApiClient apiClient) async {
    setState(() {
      function = null;
    });
    final f = await apiClient.getFunction(widget.functionId);
    setState(() {
      function = f;
    });
  }

  static const loading = Center(child: ChainlessLoadingIndicator());

  Widget deleteButton(BuildContext context) {
    onPressed() {
      widget.deleteFunction();
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => const ListFunctionsPage(),
        ),
        (_) => true,
      );
    }

    return IconButton(
      icon: const Icon(Icons.delete),
      color: Colors.orange,
      onPressed: onPressed,
      tooltip: "Permanently Delete",
    );
  }
}
