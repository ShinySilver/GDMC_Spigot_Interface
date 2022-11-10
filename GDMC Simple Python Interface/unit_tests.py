from gdmc_interface import set_block, get_block, sync, commit, status
import unittest
import sys
from datetime import datetime

backup_stdout = sys.stdout
sys.tracebacklimit = 0

# return codes
return_code_ok = 200
return_code_bad_request = 400
return_code_precondition_required = 428


class TestGDMCInterface(unittest.TestCase):

    def test_block_manipulation(self):
        """
        This unit test will place a full diamond block at (0, 0, 0), commit the change to be applied by FAWE, use sync
        to wait for the change to be applied, and then check that the block was indeed changed using get_block. At last,
        (0, 0, 0) is restored to stone.

        :return: None
        """
        with self.subTest("Setting a block"):
            request = set_block(0, 0, 0, "minecraft:diamond_block")
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')
        with self.subTest("Creating a commit"):
            request = commit(commit_name="Unit_test_"+datetime.now().strftime("%H:%M:%S"))
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')
        with self.subTest("Syncing"):
            request = sync(timeout=3)
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')
        with self.subTest("Getting a block"):
            request = get_block(0, 0, 0)
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')
            self.assertEqual("minecraft:diamond_block", request.text)
        with self.subTest("Resetting tested block"):
            request = set_block(0, 0, 0, "minecraft:bedrock")
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')
        with self.subTest("Creating a commit again"):
            request = commit()
            self.assertEqual(return_code_ok, request.status_code,
                             msg=f'Expected return code 200, got {request.status_code} with message: "{request.text}"')

    """def test_run_command(self):
        request = run_command("say Hello World!")
        self.assertEqual("", request.text)
        self.assertEqual(return_code_ok, request.status_code)

        request = run_command(run_command("BWAKBWAKBWAK"))
        self.assertEqual("Unknown command. Type \"/help\" for help.", request.text)
        self.assertEqual(return_code_ok, request.status_code)"""


if __name__ == '__main__':
    unittest.main(verbosity=2, exit=False)