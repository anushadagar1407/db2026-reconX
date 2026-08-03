import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const frontendDirectory = fileURLToPath(new URL('..', import.meta.url));
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const phases = ['lint', 'test:ci', 'build'];
const warningPhases = new Set(['lint']);
let verificationFailed = false;
let verificationWarnings = false;

function reportPhaseFailure(phase, reason) {
  if (warningPhases.has(phase)) {
    verificationWarnings = true;
    console.warn(`[verify] ${phase} reported warnings: ${reason}`);
    return;
  }

  verificationFailed = true;
  console.error(`[verify] ${phase} failed: ${reason}`);
}

for (const phase of phases) {
  console.log(`\n[verify] Running ${phase}`);

  let result;
  try {
    result = spawnSync(npmCommand, ['run', phase], {
      cwd: frontendDirectory,
      stdio: 'inherit',
    });
  } catch (error) {
    reportPhaseFailure(phase, `could not start: ${error.message}`);
    continue;
  }

  if (result.error || result.status !== 0) {
    const reason = result.error?.message ?? `exit code ${result.status ?? `signal ${result.signal}`}`;
    reportPhaseFailure(phase, reason);
  }
}

if (verificationFailed) {
  console.error('\n[verify] Verification failed.');
  process.exitCode = 1;
} else {
  const suffix = verificationWarnings ? ' with lint warnings.' : '.';
  console.log(`\n[verify] Verification passed${suffix}`);
}
