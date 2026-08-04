import { Profiler } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api } from '@services/apiService.js';
import { AddTrade } from './AddTrade.jsx';

vi.mock('@services/apiService.js', () => ({
  api: {
    createTrade: vi.fn(),
  },
}));

function currentDate() {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

async function fillValidForm(user) {
  await user.type(screen.getByLabelText('Trade ref'), 'EQU-20260803-0001');
  await user.type(screen.getByLabelText('Instrument ID'), '1');
  await user.type(screen.getByLabelText('Counterparty ID'), '2');
  await user.selectOptions(screen.getByLabelText('Asset class'), 'EQUITY');
  await user.selectOptions(screen.getByLabelText('Side'), 'BUY');
  await user.type(screen.getByLabelText('Quantity'), '1000');
  await user.type(screen.getByLabelText('Price'), '125.50');
  await user.type(screen.getByLabelText('Trade date'), currentDate());
}

describe('<AddTrade>', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    api.createTrade.mockResolvedValue({ id: 42 });
  });

  it('reports every required field and does not call the API for an empty submission', async () => {
    const user = userEvent.setup();

    render(<AddTrade />);
    await user.click(screen.getByRole('button', { name: 'Submit' }));

    const alerts = await screen.findAllByRole('alert');
    expect(alerts).toHaveLength(8);
    expect(screen.getByText('Trade reference is required')).toBeInTheDocument();
    expect(screen.getByText('Instrument ID is required')).toBeInTheDocument();
    expect(screen.getByText('Counterparty ID is required')).toBeInTheDocument();
    expect(screen.getByText('Asset class is required')).toBeInTheDocument();
    expect(screen.getByText('Side is required')).toBeInTheDocument();
    expect(screen.getByText('Quantity is required')).toBeInTheDocument();
    expect(screen.getByText('Price is required')).toBeInTheDocument();
    expect(screen.getByText('Trade date is required')).toBeInTheDocument();
    expect(api.createTrade).not.toHaveBeenCalled();
  });

  it('validates the trade reference pattern and numeric boundaries before submission', async () => {
    const user = userEvent.setup();

    render(<AddTrade />);
    await user.type(screen.getByLabelText('Trade ref'), 'equ-invalid');
    await user.type(screen.getByLabelText('Instrument ID'), '1.5');
    await user.type(screen.getByLabelText('Counterparty ID'), '-2');
    await user.selectOptions(screen.getByLabelText('Asset class'), 'EQUITY');
    await user.selectOptions(screen.getByLabelText('Side'), 'BUY');
    await user.type(screen.getByLabelText('Quantity'), '-1');
    await user.type(screen.getByLabelText('Price'), '0');
    await user.type(screen.getByLabelText('Trade date'), '2999-01-01');
    await user.click(screen.getByRole('button', { name: 'Submit' }));

    expect(await screen.findByText('Trade reference must match AAA-YYYYMMDD-NNNN')).toBeInTheDocument();
    expect(screen.getByText('Instrument ID must be a whole number')).toBeInTheDocument();
    expect(screen.getByText('Counterparty ID must be positive')).toBeInTheDocument();
    expect(screen.getByText('Quantity must be positive')).toBeInTheDocument();
    expect(screen.getByText('Price must be positive')).toBeInTheDocument();
    expect(screen.getByText('Trade date cannot be in the future')).toBeInTheDocument();
    expect(api.createTrade).not.toHaveBeenCalled();
  });

  it('submits parsed values, disables while pending, and resets after success', async () => {
    const user = userEvent.setup();
    let resolveRequest;
    api.createTrade.mockReturnValue(new Promise((resolve) => {
      resolveRequest = resolve;
    }));

    render(<AddTrade />);
    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Submit' }));

    await waitFor(() => expect(screen.getByRole('button', { name: 'Submit' })).toBeDisabled());
    expect(api.createTrade).toHaveBeenCalledWith({
      tradeRef: 'EQU-20260803-0001',
      instrumentId: 1,
      counterpartyId: 2,
      assetClass: 'EQUITY',
      side: 'BUY',
      quantity: 1000,
      price: 125.5,
      tradeDate: currentDate(),
    });

    resolveRequest({ id: 42 });
    expect(await screen.findByRole('status')).toHaveTextContent('Trade created successfully.');
    expect(screen.getByLabelText('Trade ref')).toHaveValue('');
    expect(screen.getByLabelText('Instrument ID')).toHaveValue(null);
    expect(screen.getByLabelText('Counterparty ID')).toHaveValue(null);
    expect(screen.getByLabelText('Asset class')).toHaveValue('');
    expect(screen.getByLabelText('Side')).toHaveValue('');
    expect(screen.getByLabelText('Quantity')).toHaveValue(null);
    expect(screen.getByLabelText('Price')).toHaveValue(null);
    expect(screen.getByLabelText('Trade date')).toHaveValue('');
  });

  it('keeps entered values and surfaces server failures without treating them as validation errors', async () => {
    const user = userEvent.setup();
    api.createTrade.mockRejectedValue(new Error('HTTP 409: Trade reference already exists'));

    render(<AddTrade />);
    await fillValidForm(user);
    await user.click(screen.getByRole('button', { name: 'Submit' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('HTTP 409: Trade reference already exists');
    expect(screen.getByLabelText('Trade ref')).toHaveValue('EQU-20260803-0001');
    expect(screen.queryByRole('status')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Submit' })).not.toBeDisabled();
  });

  it('does not commit a React render for each uncontrolled keystroke', async () => {
    const user = userEvent.setup();
    const commits = [];

    render(
      <Profiler id="AddTrade" onRender={() => commits.push(true)}>
        <AddTrade />
      </Profiler>
    );

    const initialCommitCount = commits.length;
    await user.type(screen.getByLabelText('Trade ref'), 'EQU');

    expect(commits).toHaveLength(initialCommitCount);
  });
});
