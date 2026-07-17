{{- define "whisper.name" -}}
{{- .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "whisper.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "whisper.labels" -}}
app.kubernetes.io/name: {{ include "whisper.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "whisper.selectorLabels" -}}
app.kubernetes.io/name: {{ include "whisper.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}
