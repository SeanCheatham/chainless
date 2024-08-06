class FunctionInfo {
  final String name;
  final String language;
  final List<String> chains;
  final Map<String, String> chainStates;
  final dynamic state;
  final String? error;
  final dynamic initialized;

  FunctionInfo(
      {required this.name,
      required this.language,
      required this.chains,
      required this.chainStates,
      required this.state,
      required this.error,
      required this.initialized});

  factory FunctionInfo.fromJson(dynamic json) {
    return FunctionInfo(
        name: json["name"],
        language: json["language"],
        chains: (json["chains"] as List).cast(),
        chainStates: (json["chainStates"] as Map).cast(),
        state: json["state"],
        error: json["error"],
        initialized: json["initialized"]);
  }
}

class FunctionState {
  final Map<String, String> chainStates;
  final dynamic state;

  FunctionState({required this.chainStates, required this.state});
}

class FunctionInvocationRecord {
  final String jobId;
  final String? functionId;
  final String? userId;
  final DateTime startTime;
  final DateTime endTime;
  final Duration activeDuration;

  FunctionInvocationRecord(
      {required this.jobId,
      required this.functionId,
      required this.userId,
      required this.startTime,
      required this.endTime,
      required this.activeDuration});

  factory FunctionInvocationRecord.fromJson(dynamic json) {
    return FunctionInvocationRecord(
      jobId: json["jobId"],
      functionId: json["functionId"],
      userId: json["userId"],
      startTime: DateTime.fromMillisecondsSinceEpoch(json["startTimestampMs"]),
      endTime: DateTime.fromMillisecondsSinceEpoch(json["endTimestampMs"]),
      activeDuration: Duration(milliseconds: json["activeDurationMs"]),
    );
  }
}
