import React from 'react';
import { withAuth } from '@components/withAuth.jsx';

function Recon() {
  return (
    <section>
      <h2>Recon</h2>
      <p>Reconciliation workspace coming soon.</p>
    </section>
  );
}

export default withAuth(Recon);