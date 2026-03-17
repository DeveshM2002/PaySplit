import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { XMarkIcon, UserPlusIcon } from '@heroicons/react/24/outline';
import { groupApi } from '../../api/groups';
import { userApi } from '../../api/users';
import useAuthStore from '../../store/authStore';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import Avatar from '../../components/ui/Avatar';

export default function CreateGroupPage() {
  const navigate = useNavigate();
  const { user: currentUser } = useAuthStore();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [selectedMembers, setSelectedMembers] = useState([]);
  const [memberInput, setMemberInput] = useState('');
  const [allUsers, setAllUsers] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const inputRef = useRef(null);
  const suggestionsRef = useRef(null);

  useEffect(() => {
    const fetchAllUsers = async () => {
      try {
        const response = await userApi.getAllUsers();
        const users = response.data?.users ?? response.data ?? [];
        const list = Array.isArray(users) ? users : [];
        setAllUsers(list.filter((u) => u.id !== currentUser?.id));
      } catch {
        setAllUsers([]);
      }
    };
    fetchAllUsers();
  }, [currentUser?.id]);

  useEffect(() => {
    function handleClickOutside(e) {
      if (
        suggestionsRef.current &&
        !suggestionsRef.current.contains(e.target) &&
        inputRef.current &&
        !inputRef.current.contains(e.target)
      ) {
        setShowSuggestions(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const suggestions = memberInput.trim()
    ? allUsers.filter(
        (u) =>
          !selectedMembers.some((m) => m.id === u.id) &&
          ((u.name || '').toLowerCase().includes(memberInput.toLowerCase()) ||
            (u.email || '').toLowerCase().includes(memberInput.toLowerCase()))
      )
    : [];

  const addRegisteredMember = (user) => {
    if (!selectedMembers.some((m) => m.id === user.id)) {
      setSelectedMembers([...selectedMembers, { id: user.id, name: user.name, email: user.email, isGuest: false }]);
    }
    setMemberInput('');
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  const addCustomMember = () => {
    const trimmed = memberInput.trim();
    if (!trimmed) return;

    const exactMatch = allUsers.find(
      (u) =>
        (u.name || '').toLowerCase() === trimmed.toLowerCase() &&
        !selectedMembers.some((m) => m.id === u.id)
    );

    if (exactMatch) {
      addRegisteredMember(exactMatch);
      return;
    }

    const alreadyAdded = selectedMembers.some(
      (m) => m.isGuest && m.name.toLowerCase() === trimmed.toLowerCase()
    );
    if (alreadyAdded) {
      setMemberInput('');
      return;
    }

    const tempId = `guest_${Date.now()}_${Math.random()}`;
    setSelectedMembers([...selectedMembers, { id: tempId, name: trimmed, isGuest: true }]);
    setMemberInput('');
    setShowSuggestions(false);
    inputRef.current?.focus();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addCustomMember();
    }
  };

  const removeMember = (memberId) => {
    setSelectedMembers(selectedMembers.filter((m) => m.id !== memberId));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!name.trim()) return;
    try {
      setLoading(true);
      setError(null);

      const memberIds = selectedMembers.filter((m) => !m.isGuest).map((m) => m.id);
      const memberNames = selectedMembers.filter((m) => m.isGuest).map((m) => m.name);

      const response = await groupApi.create({
        name: name.trim(),
        description: description.trim() || undefined,
        memberIds: memberIds.length > 0 ? memberIds : undefined,
        memberNames: memberNames.length > 0 ? memberNames : undefined,
      });
      const groupId = response.data?.id ?? response.data?.group?.id;
      if (groupId) {
        navigate(`/groups/${groupId}`);
      } else {
        navigate('/groups');
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create group');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100 mb-6">
        Create Group
      </h1>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div>
          <label
            htmlFor="name"
            className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5"
          >
            Group name *
          </label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="input-field"
            placeholder="e.g. Roommates, Trip to Goa"
            required
          />
        </div>

        <div>
          <label
            htmlFor="description"
            className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5"
          >
            Description
          </label>
          <textarea
            id="description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="input-field min-h-[80px] resize-y"
            placeholder="Optional description"
            rows={3}
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
            Add members
          </label>

          {selectedMembers.length > 0 && (
            <div className="flex flex-wrap gap-2 mb-3">
              {selectedMembers.map((m) => (
                <span
                  key={m.id}
                  className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm ${
                    m.isGuest
                      ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-300'
                      : 'bg-[var(--color-primary)]/10 text-[var(--color-primary)]'
                  }`}
                >
                  {m.name}
                  {m.isGuest && (
                    <span className="text-xs opacity-70">(new)</span>
                  )}
                  <button
                    type="button"
                    onClick={() => removeMember(m.id)}
                    className="hover:bg-black/10 dark:hover:bg-white/10 rounded-full p-0.5"
                    aria-label={`Remove ${m.name}`}
                  >
                    <XMarkIcon className="w-4 h-4" />
                  </button>
                </span>
              ))}
            </div>
          )}

          <div className="relative">
            <input
              ref={inputRef}
              type="text"
              value={memberInput}
              onChange={(e) => {
                setMemberInput(e.target.value);
                setShowSuggestions(true);
              }}
              onFocus={() => setShowSuggestions(true)}
              onKeyDown={handleKeyDown}
              className="input-field"
              placeholder="Type a name and press Enter to add..."
            />

            {showSuggestions && suggestions.length > 0 && (
              <ul
                ref={suggestionsRef}
                className="absolute z-10 w-full mt-1 border border-gray-200 dark:border-gray-600 rounded-lg overflow-hidden bg-white dark:bg-gray-800 max-h-48 overflow-y-auto shadow-lg"
              >
                {suggestions.slice(0, 8).map((user) => (
                  <li key={user.id}>
                    <button
                      type="button"
                      onClick={() => addRegisteredMember(user)}
                      className="w-full px-4 py-2.5 text-left hover:bg-gray-50 dark:hover:bg-gray-700 flex items-center gap-3 transition-colors"
                    >
                      <Avatar name={user.name || user.email} size="sm" />
                      <div className="min-w-0">
                        <span className="font-medium text-gray-900 dark:text-gray-100 block truncate">
                          {user.name ?? user.email}
                        </span>
                        {user.email && user.name && (
                          <span className="text-sm text-gray-500 dark:text-gray-400 block truncate">
                            {user.email}
                          </span>
                        )}
                      </div>
                      <UserPlusIcon className="w-5 h-5 text-gray-400 ml-auto shrink-0" />
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            Type any name and press Enter. Registered users will appear as suggestions.
          </p>
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm">
            {error}
          </div>
        )}

        <button
          type="submit"
          disabled={loading || !name.trim()}
          className="btn-primary w-full py-3"
        >
          {loading ? (
            <span className="flex items-center justify-center gap-2">
              <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
              Creating...
            </span>
          ) : (
            'Create Group'
          )}
        </button>
      </form>
    </div>
  );
}
