package com.example.data

object SampleScripts {
    fun buildSamples(nowMillis: Long): List<ScriptEntity> = listOf(
        ScriptEntity(
            title = "Neon Shader Particle Engine",
            sampleDrawableName = "img_sample_shader_1794887932883",
            createdAtMillis = nowMillis - 1000L * 60 * 14,
            scriptContent = """
                // GLSL Fragment Shader — Cybernetic Particle Field
                precision highp float;
                uniform float u_time;
                uniform vec2 u_resolution;

                void main() {
                    vec2 uv = (gl_FragCoord.xy - 0.5 * u_resolution) / u_resolution.y;
                    vec3 col = vec3(0.039, 0.039, 0.059);

                    for (float i = 0.0; i < 6.0; i++) {
                        float t = u_time * 0.45 + i * 1.047;
                        vec2 pos = vec2(sin(t * 1.3), cos(t * 0.9)) * 0.35;
                        float glow = 0.014 / (length(uv - pos) + 0.008);
                        vec3 tint = mix(vec3(0.0, 0.96, 0.83), vec3(1.0, 0.0, 0.33), i / 5.0);
                        col += tint * glow;
                    }

                    gl_FragColor = vec4(col, 1.0);
                }
            """.trimIndent()
        ),
        ScriptEntity(
            title = "Quantum Edge Auth Middleware",
            sampleDrawableName = "img_sample_quantum_1794887934325",
            createdAtMillis = nowMillis - 1000L * 60 * 60 * 3,
            scriptContent = """
                // TypeScript Edge Middleware — Token & Burst Guard
                export async function verifyQuantumSession(req: Request): Promise<Response | null> {
                  const auth = req.headers.get("authorization");
                  if (!auth?.startsWith("Bearer ")) {
                    return Response.json({ error: "Missing bearer token" }, { status: 401 });
                  }

                  const token = auth.slice(7).trim();
                  const isValid = await crypto.subtle.digest(
                    "SHA-256",
                    new TextEncoder().encode(token)
                  );

                  if (!isValid) {
                    return Response.json({ error: "Invalid signature" }, { status: 403 });
                  }
                  return null; // Proceed to upstream handler
                }
            """.trimIndent()
        ),
        ScriptEntity(
            title = "Pulse CLI Task Automator",
            sampleDrawableName = "img_sample_terminal_1794887934461",
            createdAtMillis = nowMillis - 1000L * 60 * 60 * 22,
            scriptContent = """
                #!/usr/bin/env bash
                set -euo pipefail

                CYAN='\033[0;36m'
                PINK='\033[0;35m'
                RESET='\033[0m'

                echo -e "${'$'}{CYAN}[PULSE] Initializing parallel pipeline...${'$'}{RESET}"
                for stage in "lint" "bundle" "sign" "verify"; do
                  printf "  -> Running stage: %s\n" "${'$'}stage"
                  sleep 0.25
                done
                echo -e "${'$'}{PINK}[DONE] Artifact ready for deployment.${'$'}{RESET}"
            """.trimIndent()
        )
    )
}
