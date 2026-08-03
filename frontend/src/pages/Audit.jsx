import React from 'react';
import { withAuth } from '@components/withAuth.jsx';

function Audit() {
  return (
    <section>
      <h2>Audit</h2>
      <p>Audit trail view coming soon.</p>
    </section>
  );
}

export default withAuth(Audit);