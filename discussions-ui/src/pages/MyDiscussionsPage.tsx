import { CircleFeed } from '../components/CircleFeed';

export function MyDiscussionsPage() {
  return (
    <CircleFeed
      scope="MINE"
      emptyTitle="No circles yet"
      emptyBody="Host one with the Create button, or join a circle from the Discover tab."
    />
  );
}
