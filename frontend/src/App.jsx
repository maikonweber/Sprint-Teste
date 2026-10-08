import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from './api.js';
import LoginForm from './components/LoginForm.jsx';
import PersonForm from './components/PersonForm.jsx';
import PeopleTable from './components/PeopleTable.jsx';
import NationalityPanel from './components/NationalityPanel.jsx';

// sessionStorage: o token sobrevive a um refresh, mas é descartado ao fechar a aba.
const TOKEN_KEY = 'people.jwt';

export default function App() {
  const [token, setToken] = useState(() => sessionStorage.getItem(TOKEN_KEY));
  const [notice, setNotice] = useState(null);
  const [people, setPeople] = useState([]);
  const [selectedPerson, setSelectedPerson] = useState(null);
  const [loadingPeople, setLoadingPeople] = useState(false);

  const logout = useCallback((message = null) => {
    sessionStorage.removeItem(TOKEN_KEY);
    setToken(null);
    setPeople([]);
    setSelectedPerson(null);
    setNotice(message);
  }, []);

  /** Executa uma chamada autenticada e encerra a sessão se o backend responder 401. */
  const withAuth = useCallback(
    async (call) => {
      try {
        return await call(token);
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) {
          logout('Sua sessão expirou. Faça login novamente.');
        }
        throw error;
      }
    },
    [token, logout],
  );

  const loadPeople = useCallback(async () => {
    setLoadingPeople(true);
    try {
      setPeople(await withAuth(api.listPeople));
    } catch (error) {
      setNotice(error.message);
    } finally {
      setLoadingPeople(false);
    }
  }, [withAuth]);

  useEffect(() => {
    if (token) loadPeople();
  }, [token, loadPeople]);

  const handleLogin = (newToken) => {
    sessionStorage.setItem(TOKEN_KEY, newToken);
    setNotice(null);
    setToken(newToken);
  };

  const handleCreate = async (person) => {
    const created = await withAuth((t) => api.createPerson(t, person));
    setPeople((current) => [...current, created]);
  };

  const handleDelete = async (person) => {
    if (!window.confirm(`Excluir ${person.name} ${person.surname}?`)) return;
    try {
      await withAuth((t) => api.deletePerson(t, person.id));
      setPeople((current) => current.filter((p) => p.id !== person.id));
      if (selectedPerson?.id === person.id) setSelectedPerson(null);
    } catch (error) {
      setNotice(error.message);
    }
  };

  if (!token) {
    return (
      <main className="container narrow">
        <h1>Cadastro de Pessoas</h1>
        {notice && <p className="alert">{notice}</p>}
        <LoginForm onLogin={handleLogin} />
      </main>
    );
  }

  return (
    <main className="container">
      <header className="topbar">
        <h1>Cadastro de Pessoas</h1>
        <button className="secondary" onClick={() => logout()}>
          Sair
        </button>
      </header>

      {notice && (
        <p className="alert" role="alert">
          {notice}
          <button className="link" onClick={() => setNotice(null)} aria-label="Fechar aviso">
            ×
          </button>
        </p>
      )}

      <div className="grid">
        <PersonForm onCreate={handleCreate} />
        <NationalityPanel person={selectedPerson} findNationality={(id) => withAuth((t) => api.findNationality(t, id))} />
      </div>

      <PeopleTable
        people={people}
        loading={loadingPeople}
        selectedId={selectedPerson?.id}
        onRefresh={loadPeople}
        onSelect={setSelectedPerson}
        onDelete={handleDelete}
      />
    </main>
  );
}
