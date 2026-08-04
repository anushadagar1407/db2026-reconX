// TICKET-ADV122 — Lazy + Suspense for route-based code splitting
import React, { Suspense, lazy, useEffect } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from '@components/Layout.jsx';
import PageSkeleton from '@components/PageSkeleton.jsx';
import { withErrorBoundary } from '@components/withErrorBoundary.jsx';
import { announcePresentationReady } from '@services/presentationBridge.js';

const Dashboard = lazy(() => import('@pages/Dashboard.jsx'));
const Trades = lazy(() => import('@pages/Trades.jsx'));
const Recon = lazy(() => import('@pages/Recon.jsx'));
const AddTrade = lazy(() => import('@pages/AddTrade.jsx'));
const Audit = lazy(() => import('@pages/Audit.jsx'));
const Login = lazy(() => import('@pages/Login.jsx'));

function App() {
  useEffect(() => {
    announcePresentationReady();
  }, []);

  return (
    <Layout>
      <Suspense fallback={<PageSkeleton />}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Dashboard />} />
          <Route path="/trades" element={<Trades />} />
          <Route path="/recon" element={<Recon />} />
          <Route path="/trades/new" element={<AddTrade />} />
          <Route path="/audit" element={<Audit />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Suspense>
    </Layout>
  );
}

export default withErrorBoundary(App);
