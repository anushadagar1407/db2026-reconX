import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const frontendDirectory = fileURLToPath(new URL('..', import.meta.url));
const npmCommand = process.platform === 'win32' ? 'npm.cmd' : 'npm';
const phases = ['lint', 'test:ci', 'build'];
let verificationFailed = false;

for (const phase of phases) {
  console.log(`\n[verify] Running ${phase}`);

  let result;
  try {
    result = spawnSync(npmCommand, ['run', phase], {
      cwd: frontendDirectory,
      stdio: 'inherit',
    });
  } catch (error) {
    verificationFailed = true;
    console.error(`[verify] ${phase} could not start: ${error.message}`);
    continue;
  }

  if (result.error || result.status !== 0) {
    verificationFailed = true;
    const reason = result.error?.message ?? `exit code ${result.status ?? `signal ${result.signal}`}`;
    console.error(`[verify] ${phase} failed: ${reason}`);
  }
}

if (verificationFailed) {
  console.error('\n[verify] Verification failed.');
  process.exitCode = 1;
} else {
  console.log('\n[verify] Verification passed.');
}
