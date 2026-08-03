import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '@context/AuthContext.jsx';
import { ThemeProvider } from '@context/ThemeContext.jsx';
import Dashboard from './Dashboard.jsx';

vi.mock('@hooks/useTradeStream.js', () => ({
  useTradeStream: () => ({
    trades: [
      {
        id: 1,
        quantity: 10,
        price: 100,
        status: 'MATCHED',
      },
      {
        id: 2,
        quantity: 5,
        price: 200,
        status: 'UNMATCHED',
      },
      {
        id: 3,
        quantity: 2,
        price: 500,
        status: 'DISPUTED',
      },
    ],
    isConnected: true,
  }),
}));

function renderWithProviders(ui) {
  return render(
    <MemoryRouter>
      <ThemeProvider>
        <AuthContext.Provider
          value={{
            user: {
              token: 'test-token',
              role: 'ADMIN',
            },
            login: vi.fn(),
            logout: vi.fn(),
          }}
        >
          {ui}
        </AuthContext.Provider>
      </ThemeProvider>
    </MemoryRouter>,
  );
}

describe('Dashboard', () => {
  it('shows summary cards', () => {
    renderWithProviders(<Dashboard />);

    expect(
      screen.getByRole('heading', {
        name: /portfolio value/i,
      }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole('heading', {
        name: /trades streamed/i,
      }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole('heading', {
        name: /^matched$/i,
      }),
    ).toBeInTheDocument();

    expect(
      screen.getByRole('heading', {
        name: /open breaks/i,
      }),
    ).toBeInTheDocument();

    expect(screen.getByText('$3,000.00')).toBeInTheDocument();
  });
});
