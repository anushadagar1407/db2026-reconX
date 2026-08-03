import React from 'react';
import { Link } from 'react-router-dom';

export default function Layout({ children }) {
  return (
    <div className="layout">
      <header className="layout__header">
        <h1>ReconX</h1>
        <nav className="layout__nav" aria-label="Primary">
          <Link to="/">Dashboard</Link>
          <Link to="/trades">Trades</Link>
          <Link to="/recon">Recon</Link>
          <Link to="/trades/new">Add trade</Link>
          <Link to="/audit">Audit</Link>
        </nav>
      </header>
      <main className="layout__main">{children}</main>
    </div>
  );
}