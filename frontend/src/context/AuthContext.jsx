import {
  createContext,
  useContext,
  useMemo,
  useState,
} from 'react';

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const token = sessionStorage.getItem('reconx-token');
    const role = sessionStorage.getItem('reconx-role');

    if (!token) {
      return null;
    }

    return {
      token,
      role,
    };
  });

  function login(token, role) {
    sessionStorage.setItem('reconx-token', token);
    sessionStorage.setItem('reconx-role', role);

    setUser({
      token,
      role,
    });
  }

  function logout() {
    sessionStorage.removeItem('reconx-token');
    sessionStorage.removeItem('reconx-role');
    setUser(null);
  }

  const value = useMemo(
    () => ({
      user,
      login,
      logout,
    }),
    [user],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }

  return context;
}
