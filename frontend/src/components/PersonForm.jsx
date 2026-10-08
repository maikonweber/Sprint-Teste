import { useState } from 'react';

const EMPTY_PERSON = { document: '', name: '', surname: '', email: '' };

const FIELDS = [
  { name: 'document', label: 'Documento', placeholder: 'CPF (11) ou CNPJ (14) dígitos', inputMode: 'numeric' },
  { name: 'name', label: 'Nome' },
  { name: 'surname', label: 'Sobrenome' },
  { name: 'email', label: 'E-mail', type: 'email' },
];

export default function PersonForm({ onCreate }) {
  const [person, setPerson] = useState(EMPTY_PERSON);
  const [error, setError] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [success, setSuccess] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setPerson((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    setFieldErrors({});
    setSuccess(null);
    try {
      await onCreate(person);
      setSuccess(`${person.name} cadastrado(a) com sucesso.`);
      setPerson(EMPTY_PERSON);
    } catch (err) {
      setError(err.message);
      const byField = {};
      for (const { field, message } of err.fieldErrors ?? []) {
        byField[field] = byField[field] ? `${byField[field]}; ${message}` : message;
      }
      setFieldErrors(byField);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <h2>Nova pessoa</h2>
      {FIELDS.map(({ name, label, ...inputProps }) => (
        <label key={name}>
          {label}
          <input
            name={name}
            value={person[name]}
            onChange={handleChange}
            aria-invalid={Boolean(fieldErrors[name])}
            {...inputProps}
          />
          {fieldErrors[name] && <span className="field-error">{fieldErrors[name]}</span>}
        </label>
      ))}
      {error && Object.keys(fieldErrors).length === 0 && <p className="error">{error}</p>}
      {success && <p className="success">{success}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? 'Salvando…' : 'Cadastrar'}
      </button>
    </form>
  );
}
