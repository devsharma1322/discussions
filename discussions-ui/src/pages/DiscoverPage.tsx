import { CircleFeed } from '../components/CircleFeed';

export function DiscoverPage() {
  return (
    <CircleFeed
      scope="DISCOVER"
      emptyTitle="You've joined everything 🎉"
      emptyBody="Nothing left to discover. Try the search bar — it widens to every circle."
    />
  );
}
