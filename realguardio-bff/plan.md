# Instructions

We are writing the code like Kent Beck would write it. Before modifying code we consider whether tidying first would make the change easier. Commits will be separated into commits that change the behavior of the code and commits that only change the structure of the code. Write the code one test at a time. Write the test. Get it to compile. Get it to pass. Tidy after if appropriate. Mark the test as been written.

If during implementation you notice a test is needed that is not in the list, add it at the appropriate place in the list. As you complete tests, cross them off the list. Only implement enough code to make the test you just wrote pass, along with all the previous tests. If you find you have implemented too much, git revert --hard & try again.

Each commit should have all the tests passing. Under no circumstances should you erase or alter tests just to get a commit to pass. If there is a genuine bug in a test, fix the test, but note that in the commit message.

Run the tests with mise bff-test-all-no-build

Use "Written by Claude Code" in the commit message instead of "
Generated with [Claude Code](https://claude.ai/code)

Co-Authored-By: Claude <noreply@anthropic.com>"

# Tests to Write

- ~~Header.test.tsx~~
- ~~HomePageNotSignedIn.test.tsx~~
- HomePageSignedIn.test.tsx
- providers.test.tsx
- layout.test.tsx