import 'dart:convert';
import 'dart:async';
import 'package:chainless_frontend/models/models.dart';
import 'package:http/http.dart' as http;
import 'package:rxdart/rxdart.dart';
import './http_client.dart';

class PublicApiClient {
  final String baseAddress;

  static const defaultBaseAddress = "/api";

  PublicApiClient({this.baseAddress = defaultBaseAddress});

  Map<String, String> get headers {
    final base = <String, String>{};
    base.addAll(corsHeaders);
    return base;
  }

  Future<String> create(
      String name, String language, List<String> chains) async {
    final bodyJson = {"name": name, "language": language, "chains": chains};
    final response = await httpClient.post(
      Uri.parse("$baseAddress/functions"),
      body: utf8.encode(json.encode(bodyJson)),
      headers: headers,
    );
    assert(response.statusCode == 200, "HTTP Error: ${response.body}");
    final decoded = json.decode(utf8.decode(response.bodyBytes));
    final String id = decoded["id"];
    return id;
  }

  Future<void> upload(
      String functionId, Stream<List<int>> data, int length) async {
    final request = http.MultipartRequest(
        "POST", Uri.parse("$baseAddress/function-store/$functionId"))
      ..headers.addAll(headers);
    request.files.add(http.MultipartFile('file', data, length));
    final httpClient = http.Client();
    try {
      final response = await httpClient.send(request);
      assert(response.statusCode == 200);
    } finally {
      httpClient.close();
    }
  }

  Future<void> init(
      String functionId, dynamic config, int? retroactTimestampMs) async {
    final bodyJson = {
      "config": config,
      "retroactTimestampMs": retroactTimestampMs
    };
    final response = await httpClient.post(
      Uri.parse("$baseAddress/function-init/$functionId"),
      body: utf8.encode(json.encode(bodyJson)),
      headers: headers,
    );
    assert(response.statusCode == 200, "HTTP Error: ${response.body}");
  }

  Stream<String> listFunctionIds() async* {
    final request = http.Request("GET", Uri.parse("$baseAddress/functions"))
      ..headers.addAll(headers);

    final response = await httpClient.send(request);

    assert(response.statusCode == 200);

    final stream =
        response.stream.transform(utf8.decoder).transform(const LineSplitter());

    await for (final line in stream) {
      yield line;
    }
  }

  Future<void> deleteFunction(String functionId) async {
    final response = await httpClient.delete(
      Uri.parse("$baseAddress/functions/$functionId"),
      headers: headers,
    );

    assert(response.statusCode == 200);
  }

  Future<FunctionInfo> getFunction(String functionId) async {
    final response = await httpClient.get(
      Uri.parse("$baseAddress/functions/$functionId"),
      headers: headers,
    );

    assert(response.statusCode == 200);

    final decoded = json.decode(response.body);

    final function = FunctionInfo.fromJson(decoded);

    return function;
  }

  Stream<FunctionInvocationRecord> functionInvocationRecords(String functionId,
      {required DateTime after, required DateTime before}) {
    final client = makeHttpClient();

    impl() async* {
      final request = http.Request(
          "GET",
          Uri.parse(
              "$baseAddress/function-invocations/$functionId?afterTimestampMs=${after.millisecondsSinceEpoch}&beforeTimestampMs=${before.millisecondsSinceEpoch}"))
        ..headers.addAll(headers);

      final response = await client.send(request);

      assert(response.statusCode == 200);

      final stream = response.stream
          .transform(utf8.decoder)
          .transform(const LineSplitter());

      await for (final line in stream) {
        final parsed = json.decode(line);
        final record = FunctionInvocationRecord.fromJson(parsed);
        yield record;
      }
    }

    return impl()
        .doOnCancel(() => client.close())
        .doOnDone(() => client.close())
        .doOnError((_, __) => client.close());
  }

  http.Request addHeaders(http.Request base) {
    base.headers.addAll(corsHeaders);
    return base;
  }

  Stream<FunctionState> retroact(
      String code, String language, DateTime timestamp, List<String> chains) {
    final client = makeHttpClient();
    Future<Stream<List<int>>> call() async {
      final body = {
        "code": code,
        "language": language,
        "timestampMs": timestamp.millisecondsSinceEpoch,
        "chains": chains,
      };
      final bodyBytes = utf8.encode(json.encode(body));
      final request = addHeaders(
        http.Request("POST", Uri.parse("$baseAddress/retroact"))
          ..bodyBytes = bodyBytes
          ..persistentConnection = false,
      );
      final response = await client.send(request);
      assert(response.statusCode == 200);
      return response.stream;
    }

    return Stream.fromFuture(call())
        .asyncExpand((stream) => stream)
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .where((line) => line.isNotEmpty)
        .map((line) {
          final decoded = json.decode(line);
          final stateWithChains = FunctionState(
              chainStates: (decoded["chainStates"] as Map).cast(),
              state: decoded["state"]);
          return stateWithChains;
        })
        .doOnCancel(() => client.close())
        .doOnDone(() => client.close())
        .doOnError((_, __) => client.close());
  }

  Stream<FunctionState> streamed(String code, String language,
      FunctionState stateWithChains, List<String> chains) {
    final client = makeHttpClient();
    Future<Stream<List<int>>> call() async {
      final body = {
        "code": code,
        "language": language,
        "chainStates": stateWithChains.chainStates,
        "state": stateWithChains.state,
        "chains": chains,
      };
      final bodyBytes = utf8.encode(json.encode(body));
      final request = addHeaders(
        http.Request("POST", Uri.parse("$baseAddress/live"))
          ..bodyBytes = bodyBytes
          ..persistentConnection = false,
      );

      final response = await client.send(request);
      assert(response.statusCode == 200);
      return response.stream;
    }

    return Stream.fromFuture(call())
        .asyncExpand((stream) => stream)
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .where((line) => line.isNotEmpty)
        .map((line) {
          final decoded = json.decode(line);
          final stateWithChains = FunctionState(
              chainStates: (decoded["chainStates"] as Map).cast(),
              state: decoded["state"]);
          return stateWithChains;
        })
        .doOnCancel(() => client.close())
        .doOnDone(() => client.close())
        .doOnError((_, __) => client.close());
  }
}
