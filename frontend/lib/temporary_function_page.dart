import 'package:chainless_frontend/http/api_client.dart';
import 'package:chainless_frontend/widgets/app_bar_buttons.dart';
import 'package:chainless_frontend/widgets/field_with_header.dart';
import 'package:chainless_frontend/widgets/function_view.dart';
import 'package:chainless_frontend/models/models.dart';
import 'package:chainless_frontend/ui_utils.dart';
import 'package:chainless_frontend/widgets/chain_selection_dropdown.dart';
import 'package:chainless_frontend/widgets/gradient_background.dart';
import 'package:flutter/material.dart';
import 'package:omni_datetime_picker/omni_datetime_picker.dart';
import 'package:code_editor/code_editor.dart';
import 'package:provider/provider.dart';

class TemporaryFunctionPage extends StatefulWidget {
  const TemporaryFunctionPage({super.key});

  @override
  State<TemporaryFunctionPage> createState() => _TemporaryFunctionPageState();
}

class _TemporaryFunctionPageState extends State<TemporaryFunctionPage> {
  String code = _code;
  DateTime? initTime;
  late EditorModel model;
  List<String> chains = ["bitcoin", "ethereum", "apparatus"];

  @override
  void initState() {
    super.initState();
    model = EditorModel(
      styleOptions: codeEditorStyle(500),
      files: [
        FileEditor(name: "Temporary Function", language: "js", code: code)
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return ChainlessScaffold(
      appBar: ChainlessAppBar(
        title: const Text("Create Temporary Function"),
        actions: [
          const DocsButton(suffix: "/docs/temporary-functions"),
          const DiscordInviteButton(),
        ],
      ),
      body: Center(child: functionCreateForm(context)),
    );
  }

  Widget functionCreateForm(BuildContext context) => SingleChildScrollView(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 1024),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              ChainlessCard(child: editor()),
              divider,
              ChainlessCard(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Text("Settings",
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        )).pad16,
                    divider,
                    chainsField(),
                    divider,
                    timePickerField(),
                    divider,
                    runButton(context),
                  ],
                ).pad16,
              ),
            ],
          ),
        ),
      );

  static const divider =
      Divider(thickness: 0, height: 24, color: Colors.transparent);

  Widget editor() {
    return CodeEditor(
      model: model,
      formatters: const ["js"],
      onSubmit: (_, c) {
        setState(() {
          code = c;
        });
      },
    );
  }

  Widget chainsField() {
    return FieldWithHeader(
      name: "Blockchains",
      required: true,
      tooltip:
          "The blockchains from which your function will receive events.  More than one can be selected.",
      child: ChainSelectionDropdown(
        initialSelection: chains,
        selectionUpdated: (newChains) => setState(() => chains = newChains),
      ),
    );
  }

  Widget timePickerField() {
    final icon = TextButton.icon(
        onPressed: () => showOmniDateTimePicker(
              context: context,
              initialDate: initTime,
            ).then((newInitTime) => setState(() => initTime = newInitTime)),
        icon: const Icon(Icons.edit_calendar),
        label: const Text("Set Start Time"));
    final children = <Widget>[icon];
    if (initTime != null) {
      children.addAll([
        const VerticalDivider(),
        Text(
          initTime?.toIso8601String() ?? "",
          style: const TextStyle(fontStyle: FontStyle.italic),
        ),
      ]);
    }
    return FieldWithHeader(
      name: "Initialization Time",
      required: false,
      tooltip:
          "Select a time in the past to have your function initialize with historical data.",
      child: Row(mainAxisSize: MainAxisSize.min, children: children),
    );
  }

  Widget runButton(BuildContext context) {
    return TextButton.icon(
      onPressed: () => authenticatedNavigatorKey.currentState!.push(
        MaterialPageRoute(
          builder: (context) => StateViewer(
            code: code,
            initTime: initTime,
            chains: chains,
          ),
        ),
      ),
      icon: const Icon(Icons.send),
      label: const Text("Run"),
    );
  }
}

class StateViewer extends StatelessWidget {
  final String code;
  final DateTime? initTime;
  final List<String> chains;

  const StateViewer(
      {super.key,
      required this.code,
      required this.initTime,
      required this.chains});

  @override
  Widget build(BuildContext context) => ChainlessScaffold(
        appBar: ChainlessAppBar(
          title: const Text("Temporary Function"),
          actions: [
            const DocsButton(suffix: "/docs/temporary-functions"),
            const DiscordInviteButton(),
          ],
        ),
        body: Center(
          child: StreamBuilder<FunctionState>(
            stream: stream(context.watch<PublicApiClient>()),
            builder: (context, snapshot) =>
                snapshot.hasData ? stateView(snapshot.data!) : loading,
          ),
        ),
      );

  Stream<FunctionState> stream(PublicApiClient client) async* {
    late FunctionState retroacted;
    if (initTime != null) {
      await for (final next in client.retroact(code, initTime!, chains)) {
        yield next;
        retroacted = next;
      }
    } else {
      retroacted = FunctionState(chainStates: {}, state: null);
    }
    await for (final next in client.streamed(code, retroacted, chains)) {
      yield next;
    }
  }

  Widget stateView(FunctionState functionState) => FunctionView(
        functionId: "temporary",
        function: FunctionInfo(
          chainStates: functionState.chainStates,
          state: functionState.state,
          name: "Temporary Function",
          language: "js",
          chains: chains,
          error: null,
          initialized: true,
        ),
        showRecords: false,
      );

  static const loading = Center(
    child: ChainlessLoadingIndicator(),
  );
}

const _code = """
// A function which counts blocks, grouped by chain
// A temporary function accepts two arguments: the current state and a new block
(function(functionState, blockWithMeta) {

  // Start by extracting the internal function state
  // If this is the first time running the function, the state may be null
  let state = { ...functionState.state ?? {}}

  // The `blockWithMeta` object contains some commonly indexed information about blocks, like ID, height, timestamp, and chain name
  // In this case, extract the name of the chain
  // And then extract the current value associated with that chain
  let previousChainCount = state[blockWithMeta.meta.chain] ?? 0;

  // Increment the number of blocks associated with this chain
  state[blockWithMeta.meta.chain] = previousChainCount + 1;

  // Return the updated state so that it can be passed to the next invocation
  return state;
});
""";
