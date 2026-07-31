import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { withAuth } from '@components/withAuth.jsx';
import { api } from '@services/apiService.js';

const schema = yup.object({
  tradeRef: yup
    .string()
    .required('Trade reference is required')
    .matches(
      /^[A-Z]{3}-\d{8}-\d{4}$/,
      'Use the format AAA-YYYYMMDD-NNNN',
    ),

  instrumentId: yup
    .number()
    .typeError('Instrument ID must be a number')
    .integer('Instrument ID must be an integer')
    .positive('Instrument ID must be positive')
    .required('Instrument ID is required'),

  counterpartyId: yup
    .number()
    .typeError('Counterparty ID must be a number')
    .integer('Counterparty ID must be an integer')
    .positive('Counterparty ID must be positive')
    .required('Counterparty ID is required'),

  assetClass: yup
    .string()
    .oneOf(
      ['EQUITY', 'FX', 'BOND', 'DERIVATIVE'],
      'Choose a valid asset class',
    )
    .required('Asset class is required'),

  side: yup
    .string()
    .oneOf(['BUY', 'SELL'], 'Choose BUY or SELL')
    .required('Side is required'),

  quantity: yup
    .number()
    .typeError('Quantity must be a number')
    .positive('Quantity must be positive')
    .required('Quantity is required'),

  price: yup
    .number()
    .typeError('Price must be a number')
    .positive('Price must be positive')
    .required('Price is required'),

  tradeDate: yup
    .string()
    .required('Trade date is required')
    .test(
      'valid-date',
      'Enter a valid trade date',
      (value) =>
        Boolean(value) &&
        !Number.isNaN(Date.parse(`${value}T00:00:00`)),
    ),
});

function FieldError({ error }) {
  if (!error) {
    return null;
  }

  return (
    <p className="form-error" role="alert">
      {error.message}
    </p>
  );
}

function AddTrade() {
  const [serverError, setServerError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
    reset,
  } = useForm({
    resolver: yupResolver(schema),
    mode: 'onBlur',
  });

  async function onSubmit(values) {
    setServerError('');
    setSuccessMessage('');

    try {
      await api.createTrade(values);
      reset();
      setSuccessMessage('Trade created successfully.');
    } catch (error) {
      setServerError(error.message);
    }
  }

  return (
    <section>
      <h2>Add trade</h2>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="trade-form"
        noValidate
      >
        <label>
          Trade reference
          <input
            {...register('tradeRef')}
            placeholder="EQU-20260731-0001"
          />
        </label>
        <FieldError error={errors.tradeRef} />

        <label>
          Instrument ID
          <input
            type="number"
            {...register('instrumentId')}
          />
        </label>
        <FieldError error={errors.instrumentId} />

        <label>
          Counterparty ID
          <input
            type="number"
            {...register('counterpartyId')}
          />
        </label>
        <FieldError error={errors.counterpartyId} />

        <label>
          Asset class
          <select {...register('assetClass')} defaultValue="">
            <option value="" disabled>
              Select an asset class
            </option>
            <option value="EQUITY">Equity</option>
            <option value="FX">FX</option>
            <option value="BOND">Bond</option>
            <option value="DERIVATIVE">Derivative</option>
          </select>
        </label>
        <FieldError error={errors.assetClass} />

        <label>
          Side
          <select {...register('side')} defaultValue="">
            <option value="" disabled>
              Select a side
            </option>
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </label>
        <FieldError error={errors.side} />

        <label>
          Quantity
          <input
            type="number"
            step="any"
            {...register('quantity')}
          />
        </label>
        <FieldError error={errors.quantity} />

        <label>
          Price
          <input
            type="number"
            step="any"
            {...register('price')}
          />
        </label>
        <FieldError error={errors.price} />

        <label>
          Trade date
          <input
            type="date"
            {...register('tradeDate')}
          />
        </label>
        <FieldError error={errors.tradeDate} />

        {serverError && (
          <p className="form-error" role="alert">
            {serverError}
          </p>
        )}

        {successMessage && (
          <p role="status">{successMessage}</p>
        )}

        <button disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Submitting…' : 'Submit'}
        </button>
      </form>
    </section>
  );
}

export default withAuth(AddTrade);
