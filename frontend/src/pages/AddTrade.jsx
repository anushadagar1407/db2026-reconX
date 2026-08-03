// TICKET-ADV123 — React Hook Form + Yup validation.
import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

const TRADE_REF_PATTERN = /^[A-Z]{3}-\d{8}-\d{4}$/;
const ASSET_CLASSES = ['EQUITY', 'FX', 'BOND', 'DERIVATIVE'];
const SIDES = ['BUY', 'SELL'];
const DEFAULT_VALUES = {
  tradeRef: '',
  instrumentId: '',
  counterpartyId: '',
  assetClass: '',
  side: '',
  quantity: '',
  price: '',
  tradeDate: '',
};

function requiredNumber(label, { integer = false } = {}) {
  let validator = yup.number()
    .transform((value, originalValue) => (originalValue === '' ? undefined : value))
    .typeError(`${label} must be a number`)
    .positive(`${label} must be positive`)
    .required(`${label} is required`);

  if (integer) {
    validator = validator.integer(`${label} must be a whole number`);
  }

  return validator;
}

function endOfToday() {
  const today = new Date();
  today.setHours(23, 59, 59, 999);
  return today;
}

const schema = yup.object({
  tradeRef: yup.string()
    .matches(TRADE_REF_PATTERN, {
      message: 'Trade reference must match AAA-YYYYMMDD-NNNN',
      excludeEmptyString: true,
    })
    .required('Trade reference is required'),
  instrumentId: requiredNumber('Instrument ID', { integer: true }),
  counterpartyId: requiredNumber('Counterparty ID', { integer: true }),
  assetClass: yup.string()
    .oneOf(ASSET_CLASSES, 'Asset class is required')
    .required('Asset class is required'),
  side: yup.string()
    .oneOf(SIDES, 'Side is required')
    .required('Side is required'),
  quantity: requiredNumber('Quantity'),
  price: requiredNumber('Price'),
  tradeDate: yup.date()
    .transform((value, originalValue) => (originalValue === '' ? undefined : value))
    .typeError('Trade date must be a valid date')
    .max(endOfToday(), 'Trade date cannot be in the future')
    .required('Trade date is required'),
});

function formatTradeDate(value) {
  if (!(value instanceof Date)) return value;

  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, '0');
  const day = String(value.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function AddTrade() {
  const { register, handleSubmit, formState: { errors, isSubmitting }, reset } =
        useForm({
          resolver: yupResolver(schema),
          mode: 'onBlur',
          defaultValues: DEFAULT_VALUES,
        });
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  async function onSubmit(values) {
    setServerError('');
    setSuccessMessage('');

    try {
      await api.createTrade({ ...values, tradeDate: formatTradeDate(values.tradeDate) });
      reset();
      setSuccessMessage('Trade created successfully.');
    } catch (error) {
      setServerError(error?.message || String(error || 'Unable to create trade.'));
    }
  }

  return (
    <section>
      <h2>Add trade</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="trade-form" noValidate>
        <label htmlFor="trade-ref">Trade ref</label>
        <input
          id="trade-ref"
          {...register('tradeRef')}
          placeholder="EQU-20260603-0001"
          aria-invalid={Boolean(errors.tradeRef)}
          aria-describedby={errors.tradeRef ? 'trade-ref-error' : undefined}
        />
        {errors.tradeRef && <p id="trade-ref-error" className="form-error" role="alert">{errors.tradeRef.message}</p>}

        <label htmlFor="instrument-id">Instrument ID</label>
        <input
          id="instrument-id"
          type="number"
          {...register('instrumentId')}
          aria-invalid={Boolean(errors.instrumentId)}
          aria-describedby={errors.instrumentId ? 'instrument-id-error' : undefined}
        />
        {errors.instrumentId && <p id="instrument-id-error" className="form-error" role="alert">{errors.instrumentId.message}</p>}

        <label htmlFor="counterparty-id">Counterparty ID</label>
        <input
          id="counterparty-id"
          type="number"
          {...register('counterpartyId')}
          aria-invalid={Boolean(errors.counterpartyId)}
          aria-describedby={errors.counterpartyId ? 'counterparty-id-error' : undefined}
        />
        {errors.counterpartyId && <p id="counterparty-id-error" className="form-error" role="alert">{errors.counterpartyId.message}</p>}

        <label htmlFor="asset-class">Asset class</label>
        <select
          id="asset-class"
          {...register('assetClass')}
          aria-invalid={Boolean(errors.assetClass)}
          aria-describedby={errors.assetClass ? 'asset-class-error' : undefined}
        >
          <option value="">Select an asset class</option>
          {ASSET_CLASSES.map((assetClass) => <option key={assetClass} value={assetClass}>{assetClass}</option>)}
        </select>
        {errors.assetClass && <p id="asset-class-error" className="form-error" role="alert">{errors.assetClass.message}</p>}

        <label htmlFor="side">Side</label>
        <select
          id="side"
          {...register('side')}
          aria-invalid={Boolean(errors.side)}
          aria-describedby={errors.side ? 'side-error' : undefined}
        >
          <option value="">Select a side</option>
          {SIDES.map((side) => <option key={side} value={side}>{side}</option>)}
        </select>
        {errors.side && <p id="side-error" className="form-error" role="alert">{errors.side.message}</p>}

        <label htmlFor="quantity">Quantity</label>
        <input
          id="quantity"
          type="number"
          step="0.0001"
          {...register('quantity')}
          aria-invalid={Boolean(errors.quantity)}
          aria-describedby={errors.quantity ? 'quantity-error' : undefined}
        />
        {errors.quantity && <p id="quantity-error" className="form-error" role="alert">{errors.quantity.message}</p>}

        <label htmlFor="price">Price</label>
        <input
          id="price"
          type="number"
          step="0.0001"
          {...register('price')}
          aria-invalid={Boolean(errors.price)}
          aria-describedby={errors.price ? 'price-error' : undefined}
        />
        {errors.price && <p id="price-error" className="form-error" role="alert">{errors.price.message}</p>}

        <label htmlFor="trade-date">Trade date</label>
        <input
          id="trade-date"
          type="date"
          {...register('tradeDate')}
          aria-invalid={Boolean(errors.tradeDate)}
          aria-describedby={errors.tradeDate ? 'trade-date-error' : undefined}
        />
        {errors.tradeDate && <p id="trade-date-error" className="form-error" role="alert">{errors.tradeDate.message}</p>}

        {serverError && <p className="form-error" role="alert">{serverError}</p>}
        {successMessage && <p role="status" aria-live="polite">{successMessage}</p>}

        <button disabled={isSubmitting} type="submit">Submit</button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);
